package com.quickserve.modules.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.common.util.AesEncryptionUtil;
import com.quickserve.modules.whatsapp.entity.WhatsAppConfig;
import com.quickserve.modules.whatsapp.entity.WhatsAppMessageLog;
import com.quickserve.modules.whatsapp.repository.WhatsAppConfigRepository;
import com.quickserve.modules.whatsapp.repository.WhatsAppWebhookEventRepository;
import com.quickserve.modules.whatsapp.service.WhatsAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.quickserve.common.events.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@Tag(name = "WhatsApp", description = "WhatsApp Business setup and webhooks")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppConfigRepository      configRepository;
    private final WhatsAppService               whatsAppService;
    private final AesEncryptionUtil             aesUtil;
    private final EventPublisher                eventPublisher;
    private final WhatsAppWebhookEventRepository webhookEventRepository;
    private final ObjectMapper             objectMapper;

    @Value("${quickserve.whatsapp.webhook-verify-token}")
    private String webhookVerifyToken;

    // ===== ONBOARDING SETUP =====

    @PostMapping("/api/onboarding/whatsapp/config")
    public ResponseEntity<ApiResponse<WhatsAppConfig>> saveConfig(@Valid @RequestBody WaConfigRequest req) {
        UUID businessId = TenantContext.getBusinessId();
        WhatsAppConfig config = configRepository.findByBusinessId(businessId)
                .orElse(new WhatsAppConfig());

        config.setBusinessId(businessId);
        config.setPhoneNumberId(req.getPhoneNumberId());
        config.setWabaId(req.getWabaId());
        config.setAccessTokenEncrypted(aesUtil.encrypt(req.getAccessToken()));
        config.setVerifiedNumber(req.getVerifiedNumber());
        config.setActive(true);
        config = configRepository.save(config);

        // Send test message
        whatsAppService.sendTemplateMessage(
                businessId, req.getVerifiedNumber(), "order_confirmation",
                List.of("Owner", "TEST-001", "Your Business", "Setup test", "0", "https://quickserve.in")
        );

        return ResponseEntity.ok(ApiResponse.ok("WhatsApp connected successfully", config));
    }

    @PutMapping("/api/onboarding/whatsapp/notifications")
    public ResponseEntity<ApiResponse<WhatsAppConfig>> updateNotifications(
            @RequestBody Map<String, Boolean> prefs) {
        UUID businessId = TenantContext.getBusinessId();
        WhatsAppConfig config = configRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("WhatsAppConfig", businessId));

        if (prefs.containsKey("orderConfirmation"))  config.setNotifyOrderConfirmation(prefs.get("orderConfirmation"));
        if (prefs.containsKey("invoiceDelivery"))    config.setNotifyInvoiceDelivery(prefs.get("invoiceDelivery"));
        if (prefs.containsKey("lowStockAlert"))      config.setNotifyLowStock(prefs.get("lowStockAlert"));
        if (prefs.containsKey("dailySalesSummary"))  config.setNotifyDailySummary(prefs.get("dailySalesSummary"));
        if (prefs.containsKey("payslipDelivery"))    config.setNotifyPayslip(prefs.get("payslipDelivery"));
        if (prefs.containsKey("loyaltyUpdate"))      config.setNotifyLoyaltyUpdate(prefs.get("loyaltyUpdate"));

        return ResponseEntity.ok(ApiResponse.ok(configRepository.save(config)));
    }

    // ===== WEBHOOK =====

    @GetMapping("/api/webhooks/whatsapp")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && webhookVerifyToken.equals(verifyToken)) {
            log.info("WhatsApp webhook verified");
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    @PostMapping("/api/webhooks/whatsapp")
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawPayload) {
        // Always return 200 immediately
        try {
            // Save raw event for debugging
            var webhookEvent = new com.quickserve.modules.whatsapp.entity.WhatsAppWebhookEvent();
            webhookEvent.setPayload(rawPayload);
            webhookEventRepository.save(webhookEvent);

            // Parse and process
            Map<?, ?> payload = objectMapper.readValue(rawPayload, Map.class);
            processWebhookPayload(payload);
        } catch (Exception ex) {
            log.error("WhatsApp webhook processing error: {}", ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private void processWebhookPayload(Map<?, ?> payload) {
        try {
            var entry = (List<?>) payload.get("entry");
            if (entry == null) return;
            for (var e : entry) {
                var changes = (List<?>) ((Map<?, ?>) e).get("changes");
                if (changes == null) continue;
                for (var c : changes) {
                    var value = (Map<?, ?>) ((Map<?, ?>) c).get("value");
                    if (value == null) continue;

                    // Status updates
                    var statuses = (List<?>) value.get("statuses");
                    if (statuses != null) {
                        for (var s : statuses) {
                            var statusObj = (Map<?, ?>) s;
                            String wamid  = (String) statusObj.get("id");
                            String status = (String) statusObj.get("status");
                            if (wamid != null && status != null) {
                                WhatsAppMessageLog.MessageStatus msgStatus = switch (status) {
                                    case "delivered" -> WhatsAppMessageLog.MessageStatus.DELIVERED;
                                    case "read"      -> WhatsAppMessageLog.MessageStatus.READ;
                                    case "failed"    -> WhatsAppMessageLog.MessageStatus.FAILED;
                                    default          -> null;
                                };
                                if (msgStatus != null) {
                                    whatsAppService.updateDeliveryStatus(wamid, msgStatus);
                                }
                            }
                        }
                    }

                    // Incoming messages — publish to Kafka for processing
                    var messages = (List<?>) value.get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        eventPublisher.publish("whatsapp.incoming", value);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error parsing WhatsApp webhook: {}", ex.getMessage());
        }
    }

    // ===== Request DTOs =====

    @Data
    static class WaConfigRequest {
        @NotBlank private String phoneNumberId;
        @NotBlank private String wabaId;
        @NotBlank private String accessToken;
        @NotBlank private String verifiedNumber;
    }
}
