package com.quickserve.modules.whatsapp.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "whatsapp_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppConfig extends BaseEntity {

    @Column(name = "business_id", nullable = false, unique = true)
    private UUID businessId;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "waba_id")
    private String wabaId;

    @Column(name = "access_token_encrypted")
    private String accessTokenEncrypted;

    @Column(name = "verified_number")
    private String verifiedNumber;

    @Column(name = "is_active")
    private boolean active = false;

    @Column(name = "notify_order_confirmation")
    private boolean notifyOrderConfirmation = true;

    @Column(name = "notify_order_ready")
    private boolean notifyOrderReady = true;

    @Column(name = "notify_invoice_delivery")
    private boolean notifyInvoiceDelivery = true;

    @Column(name = "notify_low_stock")
    private boolean notifyLowStock = true;

    @Column(name = "notify_daily_summary")
    private boolean notifyDailySummary = true;

    @Column(name = "notify_payslip")
    private boolean notifyPayslip = true;

    @Column(name = "notify_loyalty_update")
    private boolean notifyLoyaltyUpdate = true;

    @Column(name = "notify_reservation_confirm")
    private boolean notifyReservationConfirm = true;
}
