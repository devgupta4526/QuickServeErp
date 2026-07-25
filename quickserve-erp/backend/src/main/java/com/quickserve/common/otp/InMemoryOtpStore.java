package com.quickserve.common.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store — active when app.mock.enabled=true.
 * No Redis required. TTL is enforced via expiry timestamps.
 */
@Slf4j
@Component
@Profile("mock")
public class InMemoryOtpStore implements OtpStore {

    private static final long OTP_TTL_SECONDS     = 600;   // 10 minutes
    private static final long RESEND_TTL_SECONDS  = 3600;  // 1 hour

    private record Entry(String value, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entry> resendCounts = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String otp) {
        store.put("otp:" + key, new Entry(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
        log.info("\n╔══════════════════════════════╗\n║  [MOCK OTP] {}  →  {}  ║\n╚══════════════════════════════╝", key, otp);
    }

    @Override
    public String get(String key) {
        Entry e = store.get("otp:" + key);
        if (e == null || Instant.now().isAfter(e.expiresAt())) {
            store.remove("otp:" + key);
            return null;
        }
        return e.value();
    }

    @Override
    public void delete(String key) {
        store.remove("otp:" + key);
    }

    @Override
    public long incrementResendCount(String key) {
        String k = "resend:" + key;
        Entry existing = resendCounts.get(k);
        long newCount;
        if (existing == null || Instant.now().isAfter(existing.expiresAt())) {
            newCount = 1;
        } else {
            newCount = Long.parseLong(existing.value()) + 1;
        }
        resendCounts.put(k, new Entry(String.valueOf(newCount), Instant.now().plusSeconds(RESEND_TTL_SECONDS)));
        return newCount;
    }
}
