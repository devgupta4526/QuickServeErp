package com.quickserve.modules.onboarding.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@Tag(name = "Onboarding", description = "5-step guided business onboarding wizard")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Object>> getProgress() {
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getProgress(TenantContext.getBusinessId())));
    }

    @PutMapping("/business-profile")
    public ResponseEntity<ApiResponse<Object>> updateBusinessProfile(@RequestBody BusinessProfileRequest req) {
        var result = onboardingService.updateBusinessProfile(
                TenantContext.getBusinessId(),
                req.getBusinessType(), req.getGstin(), req.getPan(),
                req.getAddressLine1(), req.getCity(), req.getState(), req.getPincode(),
                req.isGstInclusive()
        );
        return ResponseEntity.ok(ApiResponse.ok("Business profile saved", result));
    }

    @PostMapping("/outlet")
    public ResponseEntity<ApiResponse<Object>> createOutlet(@RequestBody OutletRequest req) {
        var result = onboardingService.createOutlet(
                TenantContext.getBusinessId(),
                req.getName(), req.getOutletType(), req.getPhone(),
                req.getCity(), req.getState(), req.getPincode(),
                req.getTableCount(), req.getGstNumber(), null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Outlet created", result));
    }

    @PostMapping("/menu-skip")
    public ResponseEntity<ApiResponse<Void>> skipMenu() {
        return ResponseEntity.ok(ApiResponse.ok("Demo menu loaded", null));
    }

    @PostMapping("/whatsapp/skip")
    public ResponseEntity<ApiResponse<Void>> skipWhatsApp() {
        return ResponseEntity.ok(ApiResponse.ok("WhatsApp setup skipped", null));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Object>> complete() {
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.completOnboarding(TenantContext.getBusinessId())));
    }

    // ===== Request DTOs =====

    @Data
    static class BusinessProfileRequest {
        private String businessType;
        private String gstin;
        private String pan;
        private String addressLine1;
        private String city;
        private String state;
        private String pincode;
        private boolean gstInclusive = false;
    }

    @Data
    static class OutletRequest {
        @NotBlank private String name;
        private String outletType = "DINE_IN";
        private String phone;
        private String city;
        private String state;
        private String pincode;
        @Min(0)  private int tableCount = 0;
        private String gstNumber;
    }
}
