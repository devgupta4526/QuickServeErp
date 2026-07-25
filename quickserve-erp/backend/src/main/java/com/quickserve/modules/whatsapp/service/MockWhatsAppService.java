package com.quickserve.modules.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Mock WhatsApp service — active when quickserve.mock.enabled=true
 * Logs all WhatsApp messages instead of calling the Meta API.
 * No WhatsApp Business account needed.
 */
@Slf4j
@Service("whatsAppServiceMock")
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockWhatsAppService {

    public boolean sendTemplateMessage(UUID businessId, String toNumber,
                                       String templateName, List<String> variables) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  MOCK WhatsApp → {} | Template: {}  ║",
                mask(toNumber), templateName);
        for (int i = 0; i < variables.size(); i++) {
            log.info("║    {{{}}} = {}  ║", i + 1, variables.get(i));
        }
        log.info("╚══════════════════════════════════════════════════════╝");
        return true;
    }

    public boolean sendDocument(UUID businessId, String toNumber, String templateName,
                                List<String> variables, String documentUrl, String filename) {
        log.info("[MOCK WhatsApp DOCUMENT] → {} | Template: {} | Doc: {}",
                mask(toNumber), templateName, filename);
        return true;
    }

    private String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return number.substring(0, 2) + "****" + number.substring(number.length() - 2);
    }
}
