package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.res.dashboard.DashboardSummaryResponse;
import com.example.bezma.service.iService.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bezma.util.TenantContext;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Các API thống kê tổng quan hệ thống")
public class DashboardController {

    private final IDashboardService dashboardService;

    @Operation(summary = "Lấy dữ liệu tóm tắt cho Dashboard")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return ApiResponse.<DashboardSummaryResponse>builder()
                .data(dashboardService.getSummary(tenantId))
                .message("Lấy dữ liệu thống kê thành công!")
                .build();
    }
}
