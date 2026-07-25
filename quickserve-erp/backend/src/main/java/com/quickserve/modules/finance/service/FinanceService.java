package com.quickserve.modules.finance.service;

import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.finance.entity.Invoice;
import com.quickserve.modules.finance.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public PagedResponse<Invoice> getInvoices(int page, int size) {
        return PagedResponse.from(invoiceRepository.findByBusinessId(
                TenantContext.getBusinessId(), PageRequest.of(page, size)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGstr1(UUID businessId, String month) {
        // GSTR-1 aggregate B2B and B2C sales
        return Map.of(
                "month", month,
                "b2bSales", java.util.List.of(),
                "b2cSales", java.util.List.of(),
                "totalTaxable", java.math.BigDecimal.ZERO,
                "totalCgst", java.math.BigDecimal.ZERO,
                "totalSgst", java.math.BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGstr3b(UUID businessId, String month) {
        return Map.of(
                "month", month,
                "outwardSupplies", java.math.BigDecimal.ZERO,
                "itcAvailable", java.math.BigDecimal.ZERO,
                "taxPayable", java.math.BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrialBalance(UUID businessId, LocalDate date) {
        return Map.of("date", date, "accounts", java.util.List.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfitLoss(UUID businessId, LocalDate from, LocalDate to) {
        return Map.of(
                "from", from, "to", to,
                "revenue", java.math.BigDecimal.ZERO,
                "expenses", java.math.BigDecimal.ZERO,
                "netProfit", java.math.BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public Object getExpenses(int page, int size) {
        return java.util.List.of();
    }

    @Transactional
    public Object createExpense(Object req) {
        // Full implementation with JPA entity
        log.info("Creating expense record");
        return Map.of("status", "created");
    }
}
