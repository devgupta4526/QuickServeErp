package com.quickserve.modules.hr.repository;

import com.quickserve.modules.hr.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(UUID employeeId, LocalDate from, LocalDate to);
    List<Attendance> findByEmployeeIdAndAttendanceDateBetweenAndStatus(
            UUID employeeId, LocalDate from, LocalDate to, Attendance.AttendanceStatus status);
    int countByEmployeeIdAndAttendanceDateBetweenAndStatus(
            UUID employeeId, LocalDate from, LocalDate to, Attendance.AttendanceStatus status);
}
