package com.quickserve.common.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST calculation utility.
 * Handles both inclusive and exclusive GST pricing.
 */
@Component
public class GstCalculator {

    /**
     * Calculate GST amount when price is exclusive (tax added on top).
     * @param baseAmount  The base price before tax
     * @param gstPercent  The GST percentage (e.g., 18)
     * @return            Total GST amount (CGST + SGST)
     */
    public BigDecimal calculateGstExclusive(BigDecimal baseAmount, BigDecimal gstPercent) {
        return baseAmount.multiply(gstPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Extract GST from inclusive price.
     * formula: gst = price - (price * 100 / (100 + gstPercent))
     */
    public BigDecimal extractGstInclusive(BigDecimal inclusivePrice, BigDecimal gstPercent) {
        BigDecimal divisor = BigDecimal.valueOf(100).add(gstPercent);
        BigDecimal baseAmount = inclusivePrice.multiply(BigDecimal.valueOf(100))
                .divide(divisor, 4, RoundingMode.HALF_UP);
        return inclusivePrice.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Extract base amount from GST-inclusive price.
     */
    public BigDecimal extractBaseFromInclusive(BigDecimal inclusivePrice, BigDecimal gstPercent) {
        BigDecimal divisor = BigDecimal.valueOf(100).add(gstPercent);
        return inclusivePrice.multiply(BigDecimal.valueOf(100))
                .divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /**
     * CGST = SGST = half of total GST (intra-state transactions).
     */
    public BigDecimal cgst(BigDecimal totalGst) {
        return totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total amount including GST (exclusive pricing).
     */
    public BigDecimal totalWithGst(BigDecimal baseAmount, BigDecimal gstPercent) {
        return baseAmount.add(calculateGstExclusive(baseAmount, gstPercent));
    }
}
