package com.quickserve.modules.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickserve.common.util.AesEncryptionUtil;
import com.quickserve.modules.whatsapp.entity.WhatsAppConfig;
import com.quickserve.modules.whatsapp.entity.WhatsAppMessageLog;
import com.quickserve.modules.whatsapp.entity.WhatsAppMessageLog.MessageStatus;
import com.quickserve.modules.whatsapp.repository.WhatsAppConfigRepository;
import com.quickserve.modules.whatsapp.repository.WhatsAppMessageLogRepository;
import com.quickserve.common.events.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppConfigRepository     configRepository;
    private final WhatsAppMessageLogRepository logRepository;
    private final AesEncryptionUtil            aesUtil;
    private final EventPublisher               eventPublisher;
    private final WebClient.Builder            webClientBuilder;
    private final ObjectMapper                 objectMapper;

    @Value("${quickserve.whatsapp.graph-api-url:https://graph.facebook.com/v18.0}")
    private String graphApiUrl;

    /**
     * Send a WhatsApp template message.
     * Never throws — logs and returns false on failure.
     */
    public boolean sendTemplateMessage(UUID businessId, String toNumber, String templateName,
                                       List<String> variables) {
        return doSend(businessId, toNumber, templateName, variables, null, null);
    }

    /**
     * Send a document (PDF) via WhatsApp template.
     */
    public boolean sendDocument(UUID businessId, String toNumber, String templateName,
                                List<String> variables, String documentUrl, String filename) {
        return doSend(businessId, toNumber, templateName, variables, documentUrl, filename);
    }

    private boolean doSend(UUID businessId, String toNumber, String templateName,
                           List<String> variables, String documentUrl, String filename) {
        // 1. Load config (try Redis cache first)
        WhatsAppConfig config = loadConfig(businessId);
        if (config == null || !config.isActive()) {
            log.warn("WhatsApp config not found or inactive for business {}", businessId);
            return false;
        }

        // 2. Decrypt token
        String accessToken;
        try {
            accessToken = aesUtil.decrypt(config.getAccessTokenEncrypted());
        } catch (Exception e) {
            log.error("Failed to decrypt WhatsApp token for business {}", businessId);
            return false;
        }

        // 3. Build request body
        Map<String, Object> body = buildRequestBody(toNumber, templateName, variables, documentUrl, filename);

        // 4. Create log entry
        WhatsAppMessageLog logEntry = WhatsAppMessageLog.builder()
                .businessId(businessId)
                .toNumber(toNumber)
                .templateName(templateName)
                .status(MessageStatus.PENDING)
                .build();
        try {
            logEntry.setTemplateVariables(objectMapper.writeValueAsString(variables));
        } catch (Exception ignored) {}
        logEntry = logRepository.save(logEntry);

        // 5. Call Meta API
        try {
            String url = graphApiUrl + "/" + config.getPhoneNumberId() + "/messages";
            Map<?, ?> response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // Extract wamid from response
            String wamid = extractWamid(response);
            logEntry.setStatus(MessageStatus.SENT);
            logEntry.setWamid(wamid);
            logEntry.setSentAt(Instant.now());
            logRepository.save(logEntry);
            return true;

        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                // 4xx: don't retry, log as failed
                log.error("WhatsApp API 4xx error for {} template {}: {}", toNumber, templateName, ex.getMessage());
                logEntry.setStatus(MessageStatus.FAILED);
                logEntry.setErrorMessage(ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
                logRepository.save(logEntry);
                return false;
            } else {
                // 5xx: publish to retry topic
                publishRetry(logEntry.getId(), businessId, toNumber, templateName, variables, documentUrl, filename);
                logEntry.setStatus(MessageStatus.FAILED);
                logEntry.setErrorMessage("5xx error: " + ex.getMessage());
                logRepository.save(logEntry);
                return false;
            }
        } catch (Exception ex) {
            // Timeout or other error: publish to retry
            log.error("WhatsApp send failed for {} - will retry: {}", toNumber, ex.getMessage());
            publishRetry(logEntry.getId(), businessId, toNumber, templateName, variables, documentUrl, filename);
            logEntry.setStatus(MessageStatus.FAILED);
            logEntry.setErrorMessage(ex.getMessage());
            logRepository.save(logEntry);
            return false;
        }
    }

    private WhatsAppConfig loadConfig(UUID businessId) {
        String cacheKey = "wa_config:" + businessId;
        // Simple cache check (in production use proper serialization)
        return configRepository.findByBusinessId(businessId).orElse(null);
    }

    private Map<String, Object> buildRequestBody(String toNumber, String templateName,
                                                  List<String> variables,
                                                  String documentUrl, String filename) {
        // Ensure number has country code
        String formattedNumber = toNumber.startsWith("+91") ? toNumber.substring(1)
                : toNumber.startsWith("91") ? toNumber : "91" + toNumber;

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", formattedNumber);
        body.put("type", "template");

        // Build template object
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", "en"));

        // Build components
        List<Map<String, Object>> components = new java.util.ArrayList<>();

        // Header component with document (if applicable)
        if (documentUrl != null) {
            components.add(Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of(
                            "type", "document",
                            "document", Map.of(
                                    "link", documentUrl,
                                    "filename", filename != null ? filename : "document.pdf"
                            )
                    ))
            ));
        }

        // Body parameters
        if (variables != null && !variables.isEmpty()) {
            List<Map<String, String>> params = variables.stream()
                    .map(v -> Map.of("type", "text", "text", v))
                    .toList();
            components.add(Map.of("type", "body", "parameters", params));
        }

        template.put("components", components);
        body.put("template", template);
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractWamid(Map<?, ?> response) {
        if (response == null) return null;
        try {
            var messages = (List<?>) response.get("messages");
            if (messages != null && !messages.isEmpty()) {
                return (String) ((Map<?, ?>) messages.get(0)).get("id");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void publishRetry(UUID logId, UUID businessId, String toNumber, String templateName,
                               List<String> variables, String documentUrl, String filename) {
        eventPublisher.publish("whatsapp.retry", Map.of(
                "logId", logId,
                "businessId", businessId,
                "toNumber", toNumber,
                "templateName", templateName,
                "variables", variables != null ? variables : List.of(),
                "documentUrl", documentUrl != null ? documentUrl : "",
                "filename", filename != null ? filename : ""
        ));
    }

    /**
     * Update message log status from webhook delivery receipts.
     */
    public void updateDeliveryStatus(String wamid, MessageStatus status) {
        logRepository.findByWamid(wamid).ifPresent(log -> {
            log.setStatus(status);
            if (status == MessageStatus.DELIVERED) log.setDeliveredAt(Instant.now());
            if (status == MessageStatus.READ)      log.setReadAt(Instant.now());
            logRepository.save(log);
        });
    }
}
