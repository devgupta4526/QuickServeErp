package com.quickserve.modules.auth;

import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.security.JwtTokenProvider;
import com.quickserve.modules.auth.dto.AuthDtos;
import com.quickserve.modules.auth.entity.Business;
import com.quickserve.modules.auth.entity.Role;
import com.quickserve.modules.auth.entity.User;
import com.quickserve.modules.auth.repository.BusinessRepository;
import com.quickserve.modules.auth.repository.RoleRepository;
import com.quickserve.modules.auth.repository.UserRepository;
import com.quickserve.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private BusinessRepository     businessRepository;
    @Mock private RoleRepository         roleRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtTokenProvider       jwtTokenProvider;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks private AuthService authService;

    private Role ownerRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "trialDays", 14);
        ReflectionTestUtils.setField(authService, "maxOtpResendsPerHour", 3);

        ownerRole = new Role();
        ownerRole.setId(UUID.randomUUID());
        ownerRole.setName("BUSINESS_OWNER");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ===== REGISTRATION =====

    @Test
    @DisplayName("register_success_createsBusinessAndOwner")
    void register_success_createsBusinessAndOwner() {
        var req = buildRegisterRequest();

        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(businessRepository.save(any())).thenAnswer(inv -> {
            Business b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
            return b;
        });
        when(roleRepository.findByNameAndSystemTrue("BUSINESS_OWNER")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        var result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.getBusinessId()).isNotNull();
        assertThat(result.getUserId()).isNotNull();
        verify(businessRepository).save(any(Business.class));
        verify(userRepository).save(any(User.class));
        verify(valueOps).set(startsWith("otp:"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("register_duplicateMobile_throwsBusinessException")
    void register_duplicateMobile_throwsBusinessException() {
        when(userRepository.existsByPhone("9876543210")).thenReturn(true);

        var req = buildRegisterRequest();

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mobile number already registered");
    }

    @Test
    @DisplayName("register_duplicateEmail_throwsBusinessException")
    void register_duplicateEmail_throwsBusinessException() {
        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        var req = buildRegisterRequest();

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("register_createsTrialSubscription_with14DayExpiry")
    void register_createsTrialSubscription_with14DayExpiry() {
        var req = buildRegisterRequest();
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(businessRepository.save(any())).thenAnswer(inv -> {
            Business b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
            return b;
        });
        when(roleRepository.findByNameAndSystemTrue("BUSINESS_OWNER")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        authService.register(req);

        verify(businessRepository).save(argThat(b -> {
            Business bus = (Business) b;
            return bus.getStatus() == Business.BusinessStatus.ONBOARDING
                    && bus.getTrialEndsAt() != null;
        }));
    }

    // ===== OTP VERIFICATION =====

    @Test
    @DisplayName("verifyOtp_correctOtp_issuesJwt")
    void verifyOtp_correctOtp_issuesJwt() {
        var req = new AuthDtos.OtpVerifyRequest();
        req.setMobile("9876543210");
        req.setOtp("123456");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setPhone("9876543210");
        user.setRoleId(ownerRole.getId());
        user.setBusinessId(UUID.randomUUID());

        when(valueOps.get("otp:9876543210")).thenReturn("123456");
        when(redisTemplate.delete("otp:9876543210")).thenReturn(true);
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));
        when(roleRepository.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("jwt.token.here");
        when(userRepository.save(any())).thenReturn(user);

        var result = authService.verifyOtp(req);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt.token.here");
    }

    @Test
    @DisplayName("verifyOtp_wrongOtp_throwsException")
    void verifyOtp_wrongOtp_throwsException() {
        var req = new AuthDtos.OtpVerifyRequest();
        req.setMobile("9876543210");
        req.setOtp("000000");

        when(valueOps.get("otp:9876543210")).thenReturn("123456");

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    @DisplayName("verifyOtp_expiredOtp_throwsException")
    void verifyOtp_expiredOtp_throwsException() {
        var req = new AuthDtos.OtpVerifyRequest();
        req.setMobile("9876543210");
        req.setOtp("123456");

        when(valueOps.get("otp:9876543210")).thenReturn(null);

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OTP expired");
    }

    // ===== RESEND OTP =====

    @Test
    @DisplayName("resendOtp_fourthResend_throwsRateLimitException")
    void resendOtp_fourthResend_throwsRateLimitException() {
        when(valueOps.increment("otp_resend_count:9876543210")).thenReturn(4L);

        assertThatThrownBy(() -> authService.resendOtp("9876543210"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    // ===== LOGIN =====

    @Test
    @DisplayName("login_validCredentials_returnsJwtCookie")
    void login_validCredentials_returnsJwtCookie() {
        var req = new AuthDtos.LoginRequest();
        req.setMobile("9876543210");
        req.setPassword("Password1");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setPhone("9876543210");
        user.setPasswordHash("$2a$12$hash");
        user.setActive(true);
        user.setMobileVerified(true);
        user.setRoleId(ownerRole.getId());
        user.setBusinessId(UUID.randomUUID());

        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "$2a$12$hash")).thenReturn(true);
        when(roleRepository.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("test.jwt.token");
        when(userRepository.save(any())).thenReturn(user);

        var result = authService.login(req);

        assertThat(result.getAccessToken()).isEqualTo("test.jwt.token");
        assertThat(result.getRole()).isEqualTo("BUSINESS_OWNER");
    }

    @Test
    @DisplayName("login_wrongPassword_throwsException")
    void login_wrongPassword_throwsException() {
        var req = new AuthDtos.LoginRequest();
        req.setMobile("9876543210");
        req.setPassword("WrongPass1");

        User user = new User();
        user.setPasswordHash("$2a$12$hash");
        user.setActive(true);
        user.setMobileVerified(true);

        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login_inactiveUser_throwsException")
    void login_inactiveUser_throwsException() {
        var req = new AuthDtos.LoginRequest();
        req.setMobile("9876543210");
        req.setPassword("Password1");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deactivated");
    }

    // ===== HELPERS =====

    private AuthDtos.RegisterRequest buildRegisterRequest() {
        var req = new AuthDtos.RegisterRequest();
        req.setBusinessName("Test Restaurant");
        req.setOwnerName("Test Owner");
        req.setMobile("9876543210");
        req.setEmail("test@test.com");
        req.setPassword("Password1");
        return req;
    }
}
