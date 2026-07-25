package com.quickserve.modules.analytics.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.analytics.dto.AnalyticsDtos;
import com.quickserve.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Business intelligence and reporting")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AnalyticsDtos.DashboardData>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String outletId) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getDashboard(TenantContext.getBusinessId(), from, to)));
    }

    @GetMapping("/sales-report")
    public ResponseEntity<ApiResponse<AnalyticsDtos.SalesReport>> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getSalesReport(TenantContext.getBusinessId(), from, to, groupBy)));
    }

    @GetMapping("/pl-summary")
    public ResponseEntity<ApiResponse<AnalyticsDtos.PLSummary>> getPLSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getPLSummary(TenantContext.getBusinessId(), from, to)));
    }
}
