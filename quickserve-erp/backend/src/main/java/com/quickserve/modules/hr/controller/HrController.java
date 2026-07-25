package com.quickserve.modules.hr.controller;

import com.quickserve.common.response.ApiResponse;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.hr.entity.Attendance;
import com.quickserve.modules.hr.entity.Employee;
import com.quickserve.modules.hr.service.HrService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr")
@Tag(name = "HR", description = "Employee management, attendance and payroll")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','OUTLET_MANAGER','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponse<Employee>>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PagedResponse.from(hrService.getEmployees(PageRequest.of(page, size)))));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Employee>> createEmployee(@RequestBody EmployeeRequest req) {
        Employee emp = Employee.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .designation(req.getDesignation())
                .department(req.getDepartment())
                .employmentType(req.getEmploymentType())
                .salary(req.getSalary())
                .salaryType(req.getSalaryType())
                .dateOfJoining(req.getDateOfJoining())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee created", hrService.createEmployee(emp)));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(
            @PathVariable UUID id, @RequestBody EmployeeRequest req) {
        Employee updates = Employee.builder()
                .name(req.getName()).phone(req.getPhone())
                .designation(req.getDesignation()).department(req.getDepartment())
                .salary(req.getSalary()).build();
        return ResponseEntity.ok(ApiResponse.ok("Employee updated", hrService.updateEmployee(id, updates)));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<ApiResponse<Attendance>> checkIn(@RequestBody CheckInRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.checkIn(req.getEmployeeId())));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<ApiResponse<Attendance>> checkOut(@RequestBody CheckInRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.checkOut(req.getEmployeeId())));
    }

    // ===== Request DTOs =====

    @Data
    static class EmployeeRequest {
        @NotBlank private String name;
        private String phone;
        private String email;
        private String designation;
        private String department;
        private Employee.EmploymentType employmentType = Employee.EmploymentType.FULL_TIME;
        @NotNull private BigDecimal salary;
        private Employee.SalaryType salaryType = Employee.SalaryType.MONTHLY;
        private LocalDate dateOfJoining;
    }

    @Data
    static class CheckInRequest {
        @NotNull private UUID employeeId;
    }
}
