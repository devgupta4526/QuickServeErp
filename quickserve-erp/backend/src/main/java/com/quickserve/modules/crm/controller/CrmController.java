package com.quickserve.modules.crm.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.modules.crm.entity.Customer;
import com.quickserve.modules.crm.service.CrmService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM", description = "Customer relationship management and loyalty")
@RequiredArgsConstructor
public class CrmController {

    private final CrmService crmService;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PagedResponse<Customer>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(crmService.getCustomers(page, size)));
    }

    @GetMapping("/customers/search")
    public ResponseEntity<ApiResponse<List<Customer>>> searchCustomers(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(crmService.searchCustomers(q)));
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody CustomerRequest req) {
        Customer customer = Customer.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .dateOfBirth(req.getDateOfBirth())
                .notes(req.getNotes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer created", crmService.createCustomer(customer)));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(
            @PathVariable UUID id, @RequestBody CustomerRequest req) {
        Customer updates = Customer.builder()
                .name(req.getName()).phone(req.getPhone())
                .email(req.getEmail()).notes(req.getNotes()).build();
        return ResponseEntity.ok(ApiResponse.ok(crmService.updateCustomer(id, updates)));
    }

    @PostMapping("/loyalty/earn")
    public ResponseEntity<ApiResponse<Customer>> earnPoints(@RequestBody LoyaltyRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(crmService.earnLoyaltyPoints(req.getCustomerId(), req.getAmount())));
    }

    @PostMapping("/loyalty/redeem")
    public ResponseEntity<ApiResponse<Customer>> redeemPoints(@RequestBody RedeemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(crmService.redeemLoyaltyPoints(req.getCustomerId(), req.getPoints())));
    }

    // ===== Request DTOs =====

    @Data
    static class CustomerRequest {
        @NotBlank private String name;
        private String phone;
        private String email;
        private LocalDate dateOfBirth;
        private String notes;
    }

    @Data
    static class LoyaltyRequest {
        @NotNull private UUID customerId;
        @NotNull private BigDecimal amount;
    }

    @Data
    static class RedeemRequest {
        @NotNull private UUID customerId;
        private int points;
    }
}
