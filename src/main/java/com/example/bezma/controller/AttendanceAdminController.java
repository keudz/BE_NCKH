package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.attendance.ManualAttendanceRequest;
import com.example.bezma.dto.req.attendance.UpdateAttendanceRequest;
import com.example.bezma.dto.res.attendance.AttendanceReportResponse;
import com.example.bezma.dto.res.attendance.AttendanceStatsResponse;
import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.service.iService.IAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Attendance Admin Management", description = "Các API quản lý chấm công dành cho Admin")
public class AttendanceAdminController {

    private final IAttendanceService attendanceService;

    @Operation(summary = "Lấy danh sách điểm danh hôm nay")
    @GetMapping("/today")
    public ApiResponse<List<Attendance>> getTodayAttendance(
            @RequestAttribute("tenantId") Long tenantId) {
        return ApiResponse.<List<Attendance>>builder()
                .data(attendanceService.getTodayAttendance(tenantId))
                .message("Lấy danh sách điểm danh hôm nay thành công!")
                .build();
    }

    @Operation(summary = "Lấy thống kê nhanh chấm công hôm nay")
    @GetMapping("/stats")
    public ApiResponse<AttendanceStatsResponse> getTodayStats(
            @RequestAttribute("tenantId") Long tenantId) {
        return ApiResponse.<AttendanceStatsResponse>builder()
                .data(attendanceService.getTodayStats(tenantId))
                .message("Lấy thống kê chấm công hôm nay thành công!")
                .build();
    }

    @Operation(summary = "Admin điểm danh thủ công cho nhân viên")
    @PostMapping("/manual")
    public ApiResponse<Attendance> createManualAttendance(
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody ManualAttendanceRequest request) {
        return ApiResponse.<Attendance>builder()
                .data(attendanceService.createManualAttendance(tenantId, request))
                .message("Ghi nhận điểm danh thủ công thành công!")
                .build();
    }

    @Operation(summary = "Admin cập nhật/chỉnh sửa bản ghi điểm danh")
    @PutMapping("/{id}")
    public ApiResponse<Attendance> updateAttendance(
            @PathVariable Long id,
            @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.<Attendance>builder()
                .data(attendanceService.updateAttendance(id, request))
                .message("Cập nhật bản ghi điểm danh thành công!")
                .build();
    }

    @Operation(summary = "Lấy báo cáo chấm công tháng")
    @GetMapping("/report")
    public ApiResponse<List<AttendanceReportResponse>> getMonthlyReport(
            @RequestAttribute("tenantId") Long tenantId,
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        return ApiResponse.<List<AttendanceReportResponse>>builder()
                .data(attendanceService.getMonthlyReport(tenantId, month, year))
                .message("Lấy báo cáo chấm công tháng " + month + "/" + year + " thành công!")
                .build();
    }
}
