package com.quickserve.modules.finance.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends TenantEntity {

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceType type = InvoiceType.SALES;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "cgst", nullable = false)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(name = "sgst", nullable = false)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(name = "igst", nullable = false)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(name = "total", nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "irn")
    private String irn;

    @Column(name = "e_invoice_status")
    private String eInvoiceStatus;

    @Column(name = "pdf_url")
    private String pdfUrl;

    public enum InvoiceType   { SALES, PURCHASE, CREDIT_NOTE, DEBIT_NOTE }
    public enum InvoiceStatus { DRAFT, SENT, PAID, OVERDUE, CANCELLED }
}
