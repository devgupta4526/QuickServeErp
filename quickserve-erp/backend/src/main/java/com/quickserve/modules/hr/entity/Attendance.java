package com.quickserve.modules.hr.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private java.time.Instant checkIn;

    @Column(name = "check_out")
    private java.time.Instant checkOut;

    @Column(name = "work_hours")
    private BigDecimal workHours;

    @Column(name = "overtime_hours")
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "notes")
    private String notes;

    public enum AttendanceStatus { PRESENT, ABSENT, HALF_DAY, LEAVE }
}
