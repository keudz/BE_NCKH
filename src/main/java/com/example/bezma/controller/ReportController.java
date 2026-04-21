package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.res.report.ProgressReportResponse;
import com.example.bezma.service.impl.ReportServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    // API 1: Nhân viên xem tiến độ của chính mình
    @GetMapping("/my-progress")
    public ApiResponse<ProgressReportResponse> getMyProgress() {
        return ApiResponse.<ProgressReportResponse>builder()
                .data(reportService.getMyProgress())
                .message("Lấy báo cáo cá nhân thành công")
                .build();
    }

    // API 2: Admin xem toàn bộ tiến độ công ty
    @GetMapping("/tenant-progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // CHẶN NHÂN VIÊN GỌI API NÀY
    public ApiResponse<ProgressReportResponse> getTenantProgress() {
        return ApiResponse.<ProgressReportResponse>builder()
                .data(reportService.getTenantProgress())
                .message("Lấy báo cáo tổng quan doanh nghiệp thành công")
                .build();
    }
}