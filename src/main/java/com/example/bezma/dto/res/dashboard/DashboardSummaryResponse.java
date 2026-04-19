package com.example.bezma.dto.res.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {
    private long totalEmployees;
    private long todayAttendance;
    private long pendingTasks;
    private long totalProducts;
    private double inventoryValue;
}
