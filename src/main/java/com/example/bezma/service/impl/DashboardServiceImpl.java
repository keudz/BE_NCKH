package com.example.bezma.service.impl;

import com.example.bezma.dto.res.dashboard.ActivityStatsResponse;
import com.example.bezma.dto.res.dashboard.DashboardSummaryResponse;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.entity.task.Task;
import com.example.bezma.entity.task.TaskStatus;
import com.example.bezma.repository.AttendanceRepository;
import com.example.bezma.repository.ProductRepository;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final ProductRepository productRepository;

    @Override
    public DashboardSummaryResponse getSummary(Long tenantId) {
        // 1. Tổng nhân viên
        long totalEmployees = userRepository.countByTenantId(tenantId);

        // 2. Điểm danh hôm nay (Tính từ 00:00:00 đến 23:59:59)
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        long todayAttendance = attendanceRepository.countByTenantIdAndCheckTimeBetween(tenantId, startOfDay, endOfDay);

        // 3. Công việc đang chờ / đang làm
        long pendingTasks = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.TO_DO) 
                          + taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.IN_PROGRESS);

        long totalProducts = productRepository.countByTenantId(tenantId);
        Double inventoryValue = productRepository.calculateInventoryValue(tenantId);

        // 4.5 Hiệu suất vận hành (Tỷ lệ hoàn thành task)
        long totalTasks = taskRepository.countByTenantId(tenantId);
        long completedTasks = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.DONE);
        double efficiency = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        // 5. Thống kê hoạt động tuần (7 ngày gần nhất) - Tối ưu hóa: Dùng 1 query duy nhất
        java.util.List<ActivityStatsResponse> weeklyActivity = new java.util.ArrayList<>();
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7).with(LocalTime.MIN);
        
        java.util.List<Object[]> statsResult = taskRepository.countCompletedTasksGroupByDay(tenantId, sevenDaysAgo);
        java.util.Map<String, Long> statsMap = new java.util.HashMap<>();
        for (Object[] row : statsResult) {
            statsMap.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        for (int i = 6; i >= 0; i--) {
            LocalDateTime targetDate = now.minusDays(i);
            String dateKey = targetDate.toLocalDate().toString();
            long count = statsMap.getOrDefault(dateKey, 0L);
            
            int dayIdx = targetDate.getDayOfWeek().getValue(); 
            if (dayIdx == 7) dayIdx = 0; 

            weeklyActivity.add(ActivityStatsResponse.builder()
                    .day(dayNames[dayIdx])
                    .value(count)
                    .build());
        }

        // 6. Danh sách công việc gần đây
        java.util.List<TaskResponse> recentTasks = taskRepository.findTop5ByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::mapToTaskResponse)
                .collect(java.util.stream.Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalEmployees(totalEmployees)
                .todayAttendance(todayAttendance)
                .pendingTasks(pendingTasks)
                .totalProducts(totalProducts)
                .inventoryValue(inventoryValue != null ? inventoryValue : 0.0)
                .operationalEfficiency(efficiency)
                .weeklyActivity(weeklyActivity)
                .recentTasks(recentTasks)
                .build();
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .priority(task.getPriority())
                .status(task.getStatus().name())
                .dueDate(task.getDueDate())
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : "Chưa giao")
                .createdAt(task.getCreatedAt())
                .build();
    }
}
