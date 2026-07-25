package com.quickserve.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Business name is required")
        @Size(min = 2, max = 200, message = "Business name must be 2-200 characters")
        private String businessName;

        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 200, message = "Owner name must be 2-200 characters")
        private String ownerName;

        @NotBlank(message = "Mobile is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile must be a valid 10-digit Indian mobile number")
        private String mobile;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "Password must contain at least one uppercase letter and one number"
        )
        private String password;
    }

    @Data
    public static class RegisterResponse {
        private String businessId;
        private String userId;
        private String message;
    }

    @Data
    public static class OtpVerifyRequest {
        @NotBlank(message = "Mobile is required")
        private String mobile;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        private String otp;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Mobile is required")
        private String mobile;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String accessToken;
        private String userId;
        private String businessId;
        private String outletId;
        private String role;
        private String name;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Mobile is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile must be a valid 10-digit Indian mobile number")
        private String mobile;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Mobile is required")
        private String mobile;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6)
        private String otp;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100)
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "Password must contain at least one uppercase letter and one number"
        )
        private String newPassword;
    }

    @Data
    public static class UserProfileResponse {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String businessId;
        private String outletId;
        private boolean mobileVerified;
    }
}
