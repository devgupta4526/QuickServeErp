package com.quickserve.modules.hr.repository;

import com.quickserve.modules.hr.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Page<Employee> findByBusinessId(UUID businessId, Pageable pageable);
    List<Employee> findByBusinessIdAndStatus(UUID businessId, Employee.EmploymentStatus status);
    List<Employee> findByOutletId(UUID outletId);
}
