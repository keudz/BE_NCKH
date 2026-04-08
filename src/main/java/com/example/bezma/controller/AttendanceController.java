package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.user.User;
import com.example.bezma.service.iService.IAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final IAttendanceService attendanceService;

    @PostMapping("/register-face")
    public ApiResponse<String> registerFace(
            @AuthenticationPrincipal User user, // Tự động lấy User từ JWT
            @RequestParam("photo") MultipartFile photo) {
        
        attendanceService.registerFace(user.getId(), photo);
        return ApiResponse.<String>builder()
                .message("Đăng ký khuôn mặt thành công!")
                .data("OK")
                .build();
    }

    @PostMapping("/check-in")
    public ApiResponse<Attendance> checkIn(
            @AuthenticationPrincipal User user, // Tự động lấy User từ JWT
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "lat", required = false) BigDecimal lat,
            @RequestParam(value = "lon", required = false) BigDecimal lon) {
        
        Attendance attendance = attendanceService.checkIn(user.getId(), photo, lat, lon);
        String message = attendance.getStatus().name().equals("SUCCESS") 
                ? "Chấm công thành công!" 
                : "Chấm công thất bại: " + attendance.getStatus();
                
        return ApiResponse.<Attendance>builder()
                .message(message)
                .data(attendance)
                .build();
    }

    @GetMapping("/my-history")
    public ApiResponse<List<Attendance>> getMyHistory(@AuthenticationPrincipal User user) {
        return ApiResponse.<List<Attendance>>builder()
                .data(attendanceService.getMyAttendance(user.getId()))
                .build();
    }
}
