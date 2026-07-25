package com.quickserve.integrations.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock SMS service — active when quickserve.mock.enabled=true
 * Prints OTP to logs instead of sending a real SMS.
 * No Twilio account or credentials needed.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockSmsService implements SmsService {

    @Override
    public void sendOtp(String toNumber, String otp) {
        log.info("╔══════════════════════════════════════╗");
        log.info("║  MOCK SMS — OTP for {}  ║", maskNumber(toNumber));
        log.info("║                                      ║");
        log.info("║         OTP CODE: {}               ║", otp);
        log.info("║                                      ║");
        log.info("╚══════════════════════════════════════╝");
        // Also print to System.out so it's visible even without log config
        System.out.printf("%n>>> MOCK OTP for %s: %s (also try bypass code: 123456)%n%n",
                maskNumber(toNumber), otp);
    }

    @Override
    public void sendSms(String toNumber, String message) {
        log.info("[MOCK SMS] To: {} | Message: {}", maskNumber(toNumber), message);
    }

    private String maskNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        return number.substring(0, 2) + "****" + number.substring(number.length() - 2);
    }
}
