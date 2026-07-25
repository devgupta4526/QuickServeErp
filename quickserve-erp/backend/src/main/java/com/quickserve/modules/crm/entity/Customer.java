package com.quickserve.modules.crm.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "anniversary")
    private LocalDate anniversary;

    @Column(name = "loyalty_points")
    private int loyaltyPoints = 0;

    @Column(name = "tier", nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerTier tier = CustomerTier.BRONZE;

    @Column(name = "total_spend", nullable = false)
    private BigDecimal totalSpend = BigDecimal.ZERO;

    @Column(name = "visit_count")
    private int visitCount = 0;

    @Column(name = "last_visit_at")
    private Instant lastVisitAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "whatsapp_opt_out")
    private boolean whatsappOptOut = false;

    public enum CustomerTier { BRONZE, SILVER, GOLD, PLATINUM }
}
