package com.example.bezma.repository;

import com.example.bezma.entity.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllByUserIdOrderByCheckTimeDesc(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND MONTH(a.checkTime) = :month AND YEAR(a.checkTime) = :year ORDER BY a.checkTime DESC")
    List<Attendance> getHistoryByUserAndMonth(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attendance a WHERE a.tenant.id = :tenantId AND MONTH(a.checkTime) = :month AND YEAR(a.checkTime) = :year ORDER BY a.checkTime DESC")
    List<Attendance> getTenantHistoryByMonth(@org.springframework.data.repository.query.Param("tenantId") Long tenantId, @org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("year") int year);

    long countByTenantIdAndCheckTimeBetween(Long tenantId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
