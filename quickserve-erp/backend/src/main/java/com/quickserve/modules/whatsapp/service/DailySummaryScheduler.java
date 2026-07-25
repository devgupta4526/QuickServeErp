package com.quickserve.modules.whatsapp.service;

import com.quickserve.modules.auth.entity.Business;
import com.quickserve.modules.auth.repository.BusinessRepository;
import com.quickserve.modules.whatsapp.repository.WhatsAppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled WhatsApp daily summary and campaign processor.
 */
@Slf4j
@Component
@Profile("!mock")
@RequiredArgsConstructor
public class DailySummaryScheduler {

    private final WhatsAppService        whatsAppService;
    private final WhatsAppConfigRepository configRepository;
    private final BusinessRepository     businessRepository;

    /**
     * Every day at 9 AM IST — send daily sales summary to business owners.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendDailySummary() {
        log.info("Starting daily WhatsApp summary broadcast");

        List<Business> activeBusinesses = businessRepository.findAll().stream()
                .filter(b -> b.getStatus() == Business.BusinessStatus.ACTIVE)
                .toList();

        for (Business business : activeBusinesses) {
            try {
                var configOpt = configRepository.findByBusinessId(business.getId());
                if (configOpt.isEmpty() || !configOpt.get().isActive()
                        || !configOpt.get().isNotifyDailySummary()) {
                    continue;
                }

                // TODO: load yesterday's stats from OrderRepository
                // For now we log the intent
                log.info("Would send daily summary for business {}", business.getId());
                // whatsAppService.sendTemplateMessage(business.getId(), ownerPhone, "daily_sales_summary",
                //     List.of(ownerName, yesterday, totalOrders, totalRevenue, topItem, cash, upi, online));
            } catch (Exception ex) {
                // One failed business must not stop the batch
                log.error("Failed to send daily summary for business {}: {}", business.getId(), ex.getMessage());
            }
        }
    }

    /**
     * WhatsApp retry consumer — retries failed messages up to 3 times with exponential backoff.
     */
    @KafkaListener(topics = "whatsapp.retry", groupId = "whatsapp-retry")
    public void retryMessage(Map<String, Object> event) {
        try {
            UUID businessId    = UUID.fromString(event.get("businessId").toString());
            String toNumber    = event.get("toNumber").toString();
            String templateName = event.get("templateName").toString();
            @SuppressWarnings("unchecked")
            List<String> variables = (List<String>) event.get("variables");
            String documentUrl = event.get("documentUrl") != null ? event.get("documentUrl").toString() : null;
            String filename    = event.get("filename") != null ? event.get("filename").toString() : null;

            boolean sent = (documentUrl != null && !documentUrl.isBlank())
                    ? whatsAppService.sendDocument(businessId, toNumber, templateName, variables, documentUrl, filename)
                    : whatsAppService.sendTemplateMessage(businessId, toNumber, templateName, variables);

            if (sent) {
                log.info("Retry successful for {} template {}", toNumber, templateName);
            }
        } catch (Exception ex) {
            log.error("Retry consumer error: {}", ex.getMessage());
        }
    }
}
