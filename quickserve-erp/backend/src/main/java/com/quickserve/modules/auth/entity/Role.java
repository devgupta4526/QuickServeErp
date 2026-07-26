package com.quickserve.modules.auth.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "business_id")
    private UUID businessId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "permissions")
    private String[] permissions;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}
