package com.quickserve.modules.finance.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.finance.entity.Invoice;
import com.quickserve.modules.finance.service.FinanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance", description = "Invoicing, accounts, GST and double-entry accounting")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PagedResponse<Invoice>>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getInvoices(page, size)));
    }

    @GetMapping("/gst/gstr1")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> getGstr1(@RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getGstr1(TenantContext.getBusinessId(), month)));
    }

    @GetMapping("/gst/gstr3b")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> getGstr3b(@RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getGstr3b(TenantContext.getBusinessId(), month)));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getTrialBalance(TenantContext.getBusinessId(), date)));
    }

    @GetMapping("/profit-loss")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> getProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getProfitLoss(TenantContext.getBusinessId(), from, to)));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<Object>> getExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getExpenses(page, size)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<Object>> createExpense(@RequestBody ExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(financeService.createExpense(req)));
    }

    @Data
    static class ExpenseRequest {
        @NotNull private String category;
        @NotNull private String description;
        @NotNull private BigDecimal amount;
        @NotNull private LocalDate expenseDate;
        private UUID outletId;
    }
}
