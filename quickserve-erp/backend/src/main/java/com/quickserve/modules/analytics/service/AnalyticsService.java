package com.quickserve.modules.analytics.service;

import com.quickserve.modules.analytics.dto.AnalyticsDtos;
import com.quickserve.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public AnalyticsDtos.DashboardData getDashboard(UUID businessId, LocalDate from, LocalDate to) {
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        LocalDate today    = LocalDate.now(ist);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);

        Instant todayStart     = today.atStartOfDay(ist).toInstant();
        Instant todayEnd       = today.plusDays(1).atStartOfDay(ist).toInstant();
        Instant weekStartInst  = weekStart.atStartOfDay(ist).toInstant();
        Instant monthStartInst = monthStart.atStartOfDay(ist).toInstant();

        BigDecimal todayRevenue  = safeSum(orderRepository.sumRevenueByBusinessIdAndDateRange(businessId, todayStart, todayEnd));
        BigDecimal weekRevenue   = safeSum(orderRepository.sumRevenueByBusinessIdAndDateRange(businessId, weekStartInst, todayEnd));
        BigDecimal monthRevenue  = safeSum(orderRepository.sumRevenueByBusinessIdAndDateRange(businessId, monthStartInst, todayEnd));

        long todayOrders = orderRepository.countByBusinessIdAndDateRange(businessId, todayStart, todayEnd);
        BigDecimal avgOrderValue = todayOrders > 0
                ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return AnalyticsDtos.DashboardData.builder()
                .revenue(AnalyticsDtos.RevenueMetrics.builder()
                        .today(todayRevenue)
                        .thisWeek(weekRevenue)
                        .thisMonth(monthRevenue)
                        .total(monthRevenue) // simplified
                        .growthPercent(0.0)
                        .build())
                .orders(AnalyticsDtos.OrderMetrics.builder()
                        .total(todayOrders)
                        .avgOrderValue(avgOrderValue)
                        .peakHour(12)
                        .cancelRate(0.0)
                        .build())
                .topItems(List.of())
                .paymentBreakdown(Map.of(
                        "cash", BigDecimal.ZERO,
                        "upi", BigDecimal.ZERO,
                        "card", BigDecimal.ZERO
                ))
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.SalesReport getSalesReport(UUID businessId, LocalDate from, LocalDate to, String groupBy) {
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        Instant fromInst = from.atStartOfDay(ist).toInstant();
        Instant toInst   = to.plusDays(1).atStartOfDay(ist).toInstant();

        BigDecimal totalRevenue = safeSum(
                orderRepository.sumRevenueByBusinessIdAndDateRange(businessId, fromInst, toInst));
        long totalOrders = orderRepository.countByBusinessIdAndDateRange(businessId, fromInst, toInst);

        return AnalyticsDtos.SalesReport.builder()
                .dataPoints(List.of())
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.PLSummary getPLSummary(UUID businessId, LocalDate from, LocalDate to) {
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        Instant fromInst = from.atStartOfDay(ist).toInstant();
        Instant toInst   = to.plusDays(1).atStartOfDay(ist).toInstant();

        BigDecimal revenue = safeSum(
                orderRepository.sumRevenueByBusinessIdAndDateRange(businessId, fromInst, toInst));

        return AnalyticsDtos.PLSummary.builder()
                .totalRevenue(revenue)
                .costOfGoodsSold(BigDecimal.ZERO)
                .grossProfit(revenue)
                .operatingExpenses(BigDecimal.ZERO)
                .netProfit(revenue)
                .grossMarginPercent(100.0)
                .netMarginPercent(100.0)
                .build();
    }

    private BigDecimal safeSum(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
