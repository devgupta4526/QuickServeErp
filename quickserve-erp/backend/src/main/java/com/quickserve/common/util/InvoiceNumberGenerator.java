package com.quickserve.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Generates sequential, financial-year-aware invoice numbers.
 * Format: QS/2024-25/000001
 */
@Component
public class InvoiceNumberGenerator {

    /**
     * Returns the current Indian financial year string (e.g., "2024-25").
     * Indian FY starts April 1.
     */
    public static String currentFinancialYear() {
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        return year + "-" + String.valueOf(year + 1).substring(2);
    }

    /**
     * Formats an invoice number given the sequence number.
     * @param prefix  e.g., "QS"
     * @param fy      e.g., "2024-25"
     * @param seq     e.g., 42
     * @return        e.g., "QS/2024-25/000042"
     */
    public static String format(String prefix, String fy, long seq) {
        return String.format("%s/%s/%06d", prefix, fy, seq);
    }
}
