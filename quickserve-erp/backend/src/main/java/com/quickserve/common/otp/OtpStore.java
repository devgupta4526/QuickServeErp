package com.quickserve.common.otp;

/**
 * Abstraction for OTP storage.
 * Mock profile → in-memory ConcurrentHashMap (no Redis needed).
 * Dev/Prod profile → Redis-backed with TTL.
 */
public interface OtpStore {

    /** Store an OTP for the given key (mobile number) with a 10-minute TTL. */
    void save(String key, String otp);

    /** Retrieve the OTP for a key, or null if expired/absent. */
    String get(String key);

    /** Delete the OTP (after successful verification). */
    void delete(String key);

    /** Increment and return a counter (used for resend-rate-limiting). Expires after 1 hour. */
    long incrementResendCount(String key);
}
