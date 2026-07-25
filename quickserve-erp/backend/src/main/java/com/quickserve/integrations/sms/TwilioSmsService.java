package com.quickserve.integrations.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Real Twilio SMS service — active when quickserve.mock.enabled is false or missing.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class TwilioSmsService implements SmsService {

    @Value("${quickserve.twilio.account-sid:}")
    private String accountSid;

    @Value("${quickserve.twilio.auth-token:}")
    private String authToken;

    @Value("${quickserve.twilio.from-number:+10000000000}")
    private String fromNumber;

    private final WebClient.Builder webClientBuilder;

    @Override
    public void sendOtp(String toNumber, String otp) {
        String message = "Your QuickServe OTP is: " + otp + ". Valid for 10 minutes. Do not share this code.";
        sendSms(toNumber, message);
    }

    @Override
    public void sendSms(String toNumber, String message) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("Twilio credentials not configured — SMS to {} skipped", maskNumber(toNumber));
            return;
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .bodyValue(Map.of("To", "+91" + toNumber, "From", fromNumber, "Body", message))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnError(e -> log.error("Twilio SMS failed to {}: {}", maskNumber(toNumber), e.getMessage()))
                    .subscribe(resp -> log.info("SMS sent to {}", maskNumber(toNumber)));
        } catch (Exception e) {
            log.error("SMS send error: {}", e.getMessage());
        }
    }

    private String maskNumber(String n) {
        if (n == null || n.length() < 4) return "****";
        return n.substring(0, 2) + "****" + n.substring(n.length() - 2);
    }
}
