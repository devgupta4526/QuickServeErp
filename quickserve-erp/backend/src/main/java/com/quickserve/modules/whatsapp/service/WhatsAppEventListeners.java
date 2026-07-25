package com.quickserve.modules.whatsapp.service;

import com.quickserve.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumers that trigger WhatsApp messages in response to business events.
 */
@Slf4j
@Component
@Profile("!mock")
@RequiredArgsConstructor
public class WhatsAppEventListeners {

    private final WhatsAppService whatsAppService;
    private final OrderRepository orderRepository;

    /**
     * Order status changes → notify customer on WhatsApp.
     */
    @KafkaListener(topics = "order.status.changed", groupId = "whatsapp-order-listener")
    public void onOrderStatusChanged(Map<String, Object> event) {
        try {
            UUID businessId = UUID.fromString(event.get("businessId").toString());
            String customerId = event.get("customerId") != null ? event.get("customerId").toString() : null;
            String newStatus  = event.get("newStatus") != null ? event.get("newStatus").toString() : "";
            String orderNum   = event.get("orderNumber") != null ? event.get("orderNumber").toString() : "";
            String outletName = event.get("outletName") != null ? event.get("outletName").toString() : "";

            if (customerId == null || customerId.isBlank()) return;

            // TODO: look up customer phone from CRM module
            // For now we log the intent
            log.info("Would send WhatsApp for order {} status {} to customer {}", orderNum, newStatus, customerId);

            if ("PLACED".equals(newStatus)) {
                // whatsAppService.sendTemplateMessage(businessId, customerPhone, "order_confirmation",
                //     List.of(customerName, orderNum, outletName, itemsSummary, total, trackingLink));
            } else if ("READY".equals(newStatus)) {
                // whatsAppService.sendTemplateMessage(businessId, customerPhone, "order_ready",
                //     List.of(customerName, orderNum, outletName));
            }
        } catch (Exception ex) {
            // One failed event must not stop processing
            log.error("Error processing order.status.changed for WhatsApp: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Invoice generated → send PDF via WhatsApp.
     */
    @KafkaListener(topics = "invoice.generated", groupId = "whatsapp-invoice-listener")
    public void onInvoiceGenerated(Map<String, Object> event) {
        try {
            UUID businessId     = UUID.fromString(event.get("businessId").toString());
            String customerId   = event.get("customerId") != null ? event.get("customerId").toString() : null;
            String invoiceNum   = event.get("invoiceNumber") != null ? event.get("invoiceNumber").toString() : "";
            String pdfUrl       = event.get("pdfUrl") != null ? event.get("pdfUrl").toString() : "";
            Object amountObj    = event.get("amount");
            String amount       = amountObj != null ? amountObj.toString() : "0";

            if (customerId == null || pdfUrl.isBlank()) return;

            log.info("Would send invoice {} PDF via WhatsApp to customer {}", invoiceNum, customerId);
            // whatsAppService.sendDocument(businessId, customerPhone, "invoice_delivery",
            //     List.of(customerName, invoiceNum, amount, businessName), pdfUrl, "Invoice_"+invoiceNum+".pdf");
        } catch (Exception ex) {
            log.error("Error processing invoice.generated for WhatsApp: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Payroll processed → send payslip PDF to employee.
     */
    @KafkaListener(topics = "payroll.processed", groupId = "whatsapp-payroll-listener")
    public void onPayrollProcessed(Map<String, Object> event) {
        try {
            UUID businessId    = UUID.fromString(event.get("businessId").toString());
            String employeeId  = event.get("employeeId") != null ? event.get("employeeId").toString() : null;
            String empName     = event.get("employeeName") != null ? event.get("employeeName").toString() : "";
            String netSalary   = event.get("netSalary") != null ? event.get("netSalary").toString() : "0";
            String monthYear   = event.get("monthYear") != null ? event.get("monthYear").toString() : "";
            String pdfUrl      = event.get("payslipPdfUrl") != null ? event.get("payslipPdfUrl").toString() : "";

            if (employeeId == null || pdfUrl.isBlank()) return;

            log.info("Would send payslip {} via WhatsApp to employee {}", monthYear, employeeId);
            // whatsAppService.sendDocument(businessId, employeePhone, "payslip_delivery",
            //     List.of(empName, monthYear, netSalary, businessName), pdfUrl, "Payslip_"+monthYear+".pdf");
        } catch (Exception ex) {
            log.error("Error processing payroll.processed for WhatsApp: {}", ex.getMessage(), ex);
        }
    }
}
