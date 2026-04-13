package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.user.User;

import com.example.bezma.service.iService.IAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance & AI", description = "Quản lý điểm danh và nhận diện khuôn mặt AI")
public class AttendanceController {

    private final IAttendanceService attendanceService;

    @Operation(summary = "Đăng ký khuôn mặt lần đầu (Lấy mẫu AI)")
    @PostMapping(value = "/register-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> registerFace(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("photo") MultipartFile photo) {
        
        attendanceService.registerFace(currentUser.getId(), photo);
        
        return ApiResponse.<Void>builder()
                .message("Đăng ký khuôn mặt thành công!")
                .build();
    }

    @Operation(summary = "Điểm danh bằng khuôn mặt và vị trí (Check-in)")
    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Attendance> checkIn(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("lat") BigDecimal lat,
            @RequestParam("lon") BigDecimal lon) {
        
        Attendance attendance = attendanceService.checkIn(currentUser.getId(), photo, lat, lon);
        
        String message = "Điểm danh thành công!";
        if (attendance.getStatus().name().contains("FAIL")) {
            message = "Điểm danh thất bại: " + (attendance.getNote() != null ? attendance.getNote() : "Khuôn mặt không khớp");
        }

        return ApiResponse.<Attendance>builder()
                .data(attendance)
                .message(message)
                .build();
    }

    @Operation(summary = "Lấy lịch sử điểm danh của cá nhân")
    @GetMapping("/my-history")
    public ApiResponse<List<Attendance>> getMyHistory(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<Attendance>>builder()
                .data(attendanceService.getMyAttendance(currentUser.getId()))
                .message("Lấy lịch sử điểm danh thành công!")
                .build();
    }
}
