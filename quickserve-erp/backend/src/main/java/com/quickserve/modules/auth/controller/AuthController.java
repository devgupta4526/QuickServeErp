package com.quickserve.modules.auth.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.auth.dto.AuthDtos;
import com.quickserve.modules.auth.entity.User;
import com.quickserve.modules.auth.repository.UserRepository;
import com.quickserve.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, OTP verification")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Operation(summary = "Register a new business")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.RegisterResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest req) {
        var result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. OTP sent to mobile.", result));
    }

    @Operation(summary = "Verify OTP and get JWT")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> verifyOtp(
            @Valid @RequestBody AuthDtos.OtpVerifyRequest req,
            HttpServletResponse httpResponse) {
        var result = authService.verifyOtp(req);
        setAuthCookie(httpResponse, result.getAccessToken());
        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully", result));
    }

    @Operation(summary = "Resend OTP")
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody Map<String, String> body) {
        authService.resendOtp(body.get("mobile"));
        return ResponseEntity.ok(ApiResponse.ok("OTP resent successfully", null));
    }

    @Operation(summary = "Login with phone + password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest req,
            HttpServletResponse httpResponse) {
        var result = authService.login(req);
        setAuthCookie(httpResponse, result.getAccessToken());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", result));
    }

    @Operation(summary = "Logout — clear cookie")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpResponse) {
        clearAuthCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.UserProfileResponse>> me() {
        var userId = TenantContext.getUserId();
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow();

        var resp = new AuthDtos.UserProfileResponse();
        resp.setId(user.getId().toString());
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setRole(user.getRole() != null ? user.getRole().getName() : "");
        resp.setBusinessId(user.getBusinessId() != null ? user.getBusinessId().toString() : null);
        resp.setOutletId(user.getOutletId() != null ? user.getOutletId().toString() : null);
        resp.setMobileVerified(user.isMobileVerified());
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @Operation(summary = "Forgot password — send OTP")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("mobile"));
        return ResponseEntity.ok(ApiResponse.ok("If mobile is registered, OTP has been sent", null));
    }

    @Operation(summary = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    // ============================
    // Cookie helpers
    // ============================
    private void setAuthCookie(HttpServletResponse resp, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(900); // 15 min
        cookie.setAttribute("SameSite", "Strict");
        resp.addCookie(cookie);
    }

    private void clearAuthCookie(HttpServletResponse resp) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
    }
}
