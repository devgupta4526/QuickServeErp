package com.quickserve.modules.auth.service;

import com.quickserve.common.events.EventPublisher;
import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.otp.OtpStore;
import com.quickserve.common.security.JwtTokenProvider;
import com.quickserve.integrations.sms.SmsService;
import com.quickserve.modules.auth.dto.AuthDtos;
import com.quickserve.modules.auth.entity.Business;
import com.quickserve.modules.auth.entity.Role;
import com.quickserve.modules.auth.entity.User;
import com.quickserve.modules.auth.repository.BusinessRepository;
import com.quickserve.modules.auth.repository.OutletRepository;
import com.quickserve.modules.auth.repository.RoleRepository;
import com.quickserve.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final BusinessRepository businessRepository;
    private final OutletRepository   outletRepository;
    private final RoleRepository     roleRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtTokenProvider   jwtTokenProvider;
    private final OtpStore           otpStore;   // InMemoryOtpStore (mock) or RedisOtpStore (prod)
    private final SmsService         smsService;
    private final EventPublisher     eventPublisher;

    @Value("${quickserve.app.trial-days:14}")
    private int trialDays;

    @Value("${quickserve.app.max-otp-resends-per-hour:3}")
    private int maxOtpResendsPerHour;

    /** In mock mode, any OTP attempt with this code passes. */
    @Value("${quickserve.mock.otp-bypass-code:}")
    private String otpBypassCode;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public AuthDtos.RegisterResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByPhone(req.getMobile())) {
            throw new BusinessException("Mobile number already registered", HttpStatus.CONFLICT);
        }
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        Business business = Business.builder()
                .name(req.getBusinessName())
                .status(Business.BusinessStatus.ONBOARDING)
                .trialEndsAt(Instant.now().plus(trialDays, ChronoUnit.DAYS))
                .build();
        business = businessRepository.save(business);

        Role ownerRole = roleRepository.findByNameAndSystemTrue("BUSINESS_OWNER")
                .orElseThrow(() -> new BusinessException("System roles not found — run Flyway migrations"));

        User user = User.builder()
                .businessId(business.getId())
                .roleId(ownerRole.getId())
                .name(req.getOwnerName())
                .phone(req.getMobile())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .active(true)
                .mobileVerified(false)
                .build();
        user = userRepository.save(user);

        String otp = generateAndStoreOtp(req.getMobile());
        smsService.sendOtp(req.getMobile(), otp);

        eventPublisher.publish("business.registered", business.getId().toString(),
                Map.of("businessId", business.getId(), "ownerName", req.getOwnerName()));

        AuthDtos.RegisterResponse resp = new AuthDtos.RegisterResponse();
        resp.setBusinessId(business.getId().toString());
        resp.setUserId(user.getId().toString());
        resp.setMessage("OTP sent to " + maskMobile(req.getMobile())
                + (isMockBypassEnabled() ? " [MOCK: use " + otpBypassCode + "]" : ""));
        return resp;
    }

    // ── OTP Verification ──────────────────────────────────────────────────────

    @Transactional
    public AuthDtos.LoginResponse verifyOtp(AuthDtos.OtpVerifyRequest req) {
        if (isMockBypassEnabled() && otpBypassCode.equals(req.getOtp())) {
            log.info("[MOCK] OTP bypass used for {}", maskMobile(req.getMobile()));
        } else {
            String stored = otpStore.get(req.getMobile());
            if (stored == null) {
                throw new BusinessException("OTP expired. Please request a new one.", HttpStatus.BAD_REQUEST);
            }
            if (!stored.equals(req.getOtp())) {
                throw new BusinessException("Invalid OTP", HttpStatus.BAD_REQUEST);
            }
        }
        otpStore.delete(req.getMobile());

        User user = userRepository.findByPhone(req.getMobile())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setMobileVerified(true);
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    @Transactional
    public void resendOtp(String mobile) {
        long count = otpStore.incrementResendCount(mobile);
        if (count > maxOtpResendsPerHour) {
            throw new BusinessException("Too many OTP requests. Please wait before retrying.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        String otp = generateAndStoreOtp(mobile);
        smsService.sendOtp(mobile, otp);
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByPhone(req.getMobile())
                .orElseThrow(() -> new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated. Contact your manager.", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isMobileVerified()) {
            throw new BusinessException("Mobile not verified. Please verify your OTP first.", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);
        return buildLoginResponse(user);
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String mobile) {
        userRepository.findByPhone(mobile).ifPresent(u -> {
            String otp = generateAndStoreOtp(mobile);
            smsService.sendOtp(mobile, otp);
        });
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        if (isMockBypassEnabled() && otpBypassCode.equals(req.getOtp())) {
            log.info("[MOCK] OTP bypass used for password reset");
        } else {
            String stored = otpStore.get(req.getMobile());
            if (stored == null || !stored.equals(req.getOtp())) {
                throw new BusinessException("Invalid or expired OTP", HttpStatus.BAD_REQUEST);
            }
            otpStore.delete(req.getMobile());
        }

        User user = userRepository.findByPhone(req.getMobile())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateAndStoreOtp(String mobile) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        otpStore.save(mobile, otp);
        return otp;
    }

    private AuthDtos.LoginResponse buildLoginResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName()
                : roleRepository.findById(user.getRoleId())
                        .map(Role::getName).orElse("BUSINESS_OWNER");

        String token = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getBusinessId(), user.getOutletId(), roleName);

        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setAccessToken(token);
        resp.setUserId(user.getId().toString());
        resp.setBusinessId(user.getBusinessId() != null ? user.getBusinessId().toString() : null);
        resp.setOutletId(user.getOutletId() != null ? user.getOutletId().toString() : null);
        resp.setRole(roleName);
        resp.setName(user.getName());
        return resp;
    }

    private boolean isMockBypassEnabled() {
        return otpBypassCode != null && !otpBypassCode.isBlank();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(0, 2) + "****" + mobile.substring(mobile.length() - 2);
    }
}
