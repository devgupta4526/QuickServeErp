package com.quickserve.modules.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AnalyticsDtos {

    @Data
    @Builder
    public static class DashboardData {
        private RevenueMetrics revenue;
        private OrderMetrics   orders;
        private List<TopItem>  topItems;
        private Map<String, BigDecimal> paymentBreakdown;
    }

    @Data
    @Builder
    public static class RevenueMetrics {
        private BigDecimal total;
        private BigDecimal today;
        private BigDecimal thisWeek;
        private BigDecimal thisMonth;
        private Double     growthPercent;
    }

    @Data
    @Builder
    public static class OrderMetrics {
        private long       total;
        private BigDecimal avgOrderValue;
        private int        peakHour;
        private double     cancelRate;
    }

    @Data
    @Builder
    public static class TopItem {
        private String     name;
        private int        quantity;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class SalesReport {
        private List<SalesDataPoint> dataPoints;
        private BigDecimal           totalRevenue;
        private long                 totalOrders;
    }

    @Data
    @Builder
    public static class SalesDataPoint {
        private String     period;
        private BigDecimal revenue;
        private long       orders;
    }

    @Data
    @Builder
    public static class PLSummary {
        private BigDecimal totalRevenue;
        private BigDecimal costOfGoodsSold;
        private BigDecimal grossProfit;
        private BigDecimal operatingExpenses;
        private BigDecimal netProfit;
        private double     grossMarginPercent;
        private double     netMarginPercent;
    }
}
