package com.example.bezma.service.iService;

import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.dto.req.attendance.ManualAttendanceRequest;
import com.example.bezma.dto.req.attendance.UpdateAttendanceRequest;
import com.example.bezma.dto.res.attendance.AttendanceReportResponse;
import com.example.bezma.dto.res.attendance.AttendanceStatsResponse;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

public interface IAttendanceService {
    Attendance checkIn(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon);
    Attendance checkOut(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon);
    void registerFace(Long userId, MultipartFile[] photos);
    List<Attendance> getMyAttendance(Long userId);
    List<Attendance> getHistoryByMonth(Long userId, int month, int year);
    List<Attendance> getTenantHistoryByMonth(Long tenantId, int month, int year);

    // Admin Phase 2
    List<Attendance> getTodayAttendance(Long tenantId);
    AttendanceStatsResponse getTodayStats(Long tenantId);
    Attendance createManualAttendance(Long tenantId, ManualAttendanceRequest request);
    Attendance updateAttendance(Long id, UpdateAttendanceRequest request);
    List<AttendanceReportResponse> getMonthlyReport(Long tenantId, int month, int year);
}
