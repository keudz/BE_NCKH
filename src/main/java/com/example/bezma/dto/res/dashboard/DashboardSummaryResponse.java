package com.example.bezma.dto.res.dashboard;

import com.example.bezma.dto.res.task.TaskResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {
    private long totalEmployees;
    private long todayAttendance;
    private long pendingTasks;
    private long totalProducts;
    private double inventoryValue;
    private double operationalEfficiency;
    private List<ActivityStatsResponse> weeklyActivity;
    private List<TaskResponse> recentTasks;
}
