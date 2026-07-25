package com.quickserve.modules.hr.entity;

import com.quickserve.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends TenantEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "designation")
    private String designation;

    @Column(name = "department")
    private String department;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "aadhaar_number_encrypted")
    private String aadhaarNumberEncrypted;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "ifsc")
    private String ifsc;

    @Column(name = "pf_number")
    private String pfNumber;

    @Column(name = "esi_number")
    private String esiNumber;

    @Column(name = "employment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Column(name = "salary", nullable = false)
    private BigDecimal salary = BigDecimal.ZERO;

    @Column(name = "salary_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType = SalaryType.MONTHLY;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT }
    public enum SalaryType { MONTHLY, DAILY, HOURLY }
    public enum EmploymentStatus { ACTIVE, INACTIVE, TERMINATED }
}
