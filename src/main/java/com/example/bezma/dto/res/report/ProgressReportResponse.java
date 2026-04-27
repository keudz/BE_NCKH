package com.example.bezma.dto.res.report;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProgressReportResponse {
    // Thống kê tổng quan
    private long totalTasks;
    private long todoTasks;
    private long inProgressTasks;
    private long doneTasks;
    private double completionPercentage; // Phần trăm hoàn thành (Ví dụ: 75.5%)
    private long totalEmployees;

    // Dành riêng cho Admin: Xem tiến độ của từng nhân viên
    private List<EmployeeProgress> employeeProgresses;

    // Thống kê theo ngày (cho biểu đồ)
    private List<DailyStat> dailyStats;

    // Thống kê theo tháng (cho biểu đồ)
    private List<DailyStat> monthlyStats;

    @Data
    @Builder
    public static class DailyStat {
        private String label;
        private long value;
    }

    @Data
    @Builder
    public static class EmployeeProgress {
        private Long userId;
        private String fullName;
        private String avatar;
        private long totalTasks;
        private long doneTasks;
        private double completionPercentage;
    }
}