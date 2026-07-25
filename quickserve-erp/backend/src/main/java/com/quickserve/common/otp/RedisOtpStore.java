package com.quickserve.common.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed OTP store — active when app.mock.enabled=false (dev/prod).
 */
@Component
@RequiredArgsConstructor
@Profile("!mock")
public class RedisOtpStore implements OtpStore {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String key, String otp) {
        redisTemplate.opsForValue().set("otp:" + key, otp, 10, TimeUnit.MINUTES);
    }

    @Override
    public String get(String key) {
        return (String) redisTemplate.opsForValue().get("otp:" + key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete("otp:" + key);
    }

    @Override
    public long incrementResendCount(String key) {
        String countKey = "otp_resend_count:" + key;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, 1, TimeUnit.HOURS);
        }
        return count != null ? count : 1;
    }
}
