package com.quickserve.modules.hr.service;

import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.exception.ResourceNotFoundException;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.hr.entity.Attendance;
import com.quickserve.modules.hr.entity.Employee;
import com.quickserve.modules.hr.repository.AttendanceRepository;
import com.quickserve.modules.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.quickserve.common.events.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrService {

    private final EmployeeRepository   employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventPublisher       eventPublisher;

    private static final double STANDARD_WORK_HOURS = 8.0;

    // ========== EMPLOYEES ==========

    @Transactional(readOnly = true)
    public Page<Employee> getEmployees(Pageable pageable) {
        return employeeRepository.findByBusinessId(TenantContext.getBusinessId(), pageable);
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        employee.setBusinessId(TenantContext.getBusinessId());
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(UUID id, Employee updates) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        if (!emp.getBusinessId().equals(TenantContext.getBusinessId())) {
            throw new com.quickserve.common.exception.TenantAccessException();
        }
        if (updates.getName() != null)        emp.setName(updates.getName());
        if (updates.getPhone() != null)       emp.setPhone(updates.getPhone());
        if (updates.getDesignation() != null) emp.setDesignation(updates.getDesignation());
        if (updates.getDepartment() != null)  emp.setDepartment(updates.getDepartment());
        if (updates.getSalary() != null)      emp.setSalary(updates.getSalary());
        return employeeRepository.save(emp);
    }

    // ========== ATTENDANCE ==========

    @Transactional
    public Attendance checkIn(UUID employeeId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .ifPresent(existing -> {
                    if (existing.getCheckIn() != null) {
                        throw new BusinessException("Already checked in today", HttpStatus.CONFLICT);
                    }
                });

        Attendance att = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElse(Attendance.builder()
                        .employeeId(employeeId)
                        .attendanceDate(today)
                        .status(Attendance.AttendanceStatus.PRESENT)
                        .build());
        att.setCheckIn(Instant.now());
        return attendanceRepository.save(att);
    }

    @Transactional
    public Attendance checkOut(UUID employeeId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Attendance att = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .filter(a -> a.getCheckIn() != null)
                .orElseThrow(() -> new BusinessException("No active check-in found for today"));

        if (att.getCheckOut() != null) {
            throw new BusinessException("Already checked out today", HttpStatus.CONFLICT);
        }

        att.setCheckOut(Instant.now());

        // Calculate work hours
        double hours = Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes() / 60.0;
        double overtime = Math.max(0, hours - STANDARD_WORK_HOURS);
        att.setWorkHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
        att.setOvertimeHours(BigDecimal.valueOf(overtime).setScale(2, RoundingMode.HALF_UP));

        return attendanceRepository.save(att);
    }

    // ========== PAYROLL CALCULATION ==========

    /**
     * Calculate net salary with PF/ESI/TDS deductions.
     * PF: 12% of basic (employee contribution) — only if basic > ₹0
     * ESI: 0.75% of gross — only if gross <= ₹21,000
     */
    public BigDecimal calculateNetSalary(BigDecimal basicSalary, BigDecimal hra,
                                          BigDecimal otherAllowances, int presentDays, int workingDays) {
        if (workingDays == 0) return BigDecimal.ZERO;

        BigDecimal dailyRate = basicSalary.divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
        BigDecimal earnedBasic = dailyRate.multiply(BigDecimal.valueOf(presentDays))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossSalary = earnedBasic.add(hra).add(otherAllowances);

        // PF deduction: 12% of basic
        BigDecimal pf = earnedBasic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);

        // ESI deduction: 0.75% of gross (only if gross <= 21000)
        BigDecimal esi = BigDecimal.ZERO;
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            esi = grossSalary.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
        }

        // Professional tax: ₹200/month (simplified)
        BigDecimal professionalTax = new BigDecimal("200.00");

        BigDecimal totalDeductions = pf.add(esi).add(professionalTax);
        return grossSalary.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
    }
}
