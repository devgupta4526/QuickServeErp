package com.quickserve.integrations.sms;

/**
 * Abstraction for sending SMS OTPs.
 * In production: TwilioSmsService
 * In mock/dev:   MockSmsService (logs to console — no Twilio account needed)
 */
public interface SmsService {

    /**
     * Send a 6-digit OTP via SMS.
     * Never throws — implementations must handle failures internally.
     *
     * @param toNumber  recipient mobile number (10-digit Indian, e.g. "9876543210")
     * @param otp       6-digit code
     */
    void sendOtp(String toNumber, String otp);

    /**
     * Send an arbitrary text SMS.
     */
    void sendSms(String toNumber, String message);
}
