package com.quickserve.modules.menu.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_slabs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxSlab extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "percentage", nullable = false)
    private BigDecimal percentage;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "sac_code")
    private String sacCode;

    @Column(name = "is_active")
    private boolean active = true;
}
