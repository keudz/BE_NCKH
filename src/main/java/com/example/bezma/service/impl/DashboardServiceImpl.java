package com.example.bezma.service.impl;

import com.example.bezma.dto.res.dashboard.DashboardSummaryResponse;
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

        // 4. Thống kê hàng hóa
        long totalProducts = productRepository.countByTenantId(tenantId);
        Double inventoryValue = productRepository.calculateInventoryValue(tenantId);

        return DashboardSummaryResponse.builder()
                .totalEmployees(totalEmployees)
                .todayAttendance(todayAttendance)
                .pendingTasks(pendingTasks)
                .totalProducts(totalProducts)
                .inventoryValue(inventoryValue != null ? inventoryValue : 0.0)
                .build();
    }
}
