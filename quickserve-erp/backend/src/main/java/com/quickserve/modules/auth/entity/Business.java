package com.quickserve.modules.auth.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "business_type")
    @Enumerated(EnumType.STRING)
    private BusinessType businessType;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "pan")
    private String pan;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "INR";

    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BusinessStatus status = BusinessStatus.ONBOARDING;

    @Column(name = "gst_inclusive")
    private boolean gstInclusive = false;

    @Column(name = "onboarding_step")
    private int onboardingStep = 0;

    @Column(name = "subscription_plan_id")
    private UUID subscriptionPlanId;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    public enum BusinessStatus {
        ONBOARDING, ACTIVE, SUSPENDED, CANCELLED
    }

    public enum BusinessType {
        RESTAURANT, CAFE, QSR, RETAIL, BAKERY, FRANCHISE, OTHER
    }
}
