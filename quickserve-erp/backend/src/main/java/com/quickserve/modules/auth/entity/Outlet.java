package com.quickserve.modules.auth.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "outlets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outlet extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "outlet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutletType outletType = OutletType.DINE_IN;

    @Column(name = "phone")
    private String phone;

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

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public enum OutletType {
        DINE_IN, TAKEAWAY, DELIVERY_ONLY, BOTH
    }
}
