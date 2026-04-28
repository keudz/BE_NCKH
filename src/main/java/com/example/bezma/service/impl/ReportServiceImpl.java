package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.exception.AppException;
import com.example.bezma.dto.res.report.ProgressReportResponse;
import com.example.bezma.entity.task.TaskStatus;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // 1. BÁO CÁO CÁ NHÂN (Cho Nhân viên)
    public ProgressReportResponse getMyProgress() {
        // Lấy User hiện tại
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentIdentifier) // Hoặc findByPhone tuỳ bạn set
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Long userId = currentUser.getId();
        long total = taskRepository.countByAssigneeId(userId);
        long todo = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.TO_DO);
        long inProgress = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_PROGRESS);
        long done = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.DONE);

        return ProgressReportResponse.builder()
                .totalTasks(total)
                .todoTasks(todo)
                .inProgressTasks(inProgress)
                .doneTasks(done)
                .completionPercentage(calculatePercentage(done, total))
                .build();
    }

    // 2. BÁO CÁO TỔNG QUAN DOANH NGHIỆP (Cho Admin)
    public ProgressReportResponse getTenantProgress() {
        // Lấy Admin hiện tại
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> adminOptional = userRepository.findByUsername(currentIdentifier);
        if (!adminOptional.isPresent()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        User admin = adminOptional.get();
        Long tenantId = admin.getTenant().getId();

        // Thống kê tổng công ty bằng 1 query duy nhất
        Object[] tenantStats = (Object[]) taskRepository.getTenantTaskStats(tenantId).get(0);
        long total = tenantStats[0] != null ? ((Number) tenantStats[0]).longValue() : 0;
        long todo = tenantStats[1] != null ? ((Number) tenantStats[1]).longValue() : 0;
        long inProgress = tenantStats[2] != null ? ((Number) tenantStats[2]).longValue() : 0;
        long done = tenantStats[3] != null ? ((Number) tenantStats[3]).longValue() : 0;

        // Lấy danh sách toàn bộ nhân viên của tenant (chưa bị xóa)
        List<User> allEmployees = userRepository.findAllByTenantIdAndIsDeleted(tenantId, false);
        long totalEmployees = allEmployees.size();
        List<Object[]> statsResult = taskRepository.getEmployeeTaskStats(tenantId);

        // Tạo map để tra cứu stats theo userId
        Map<Long, Object[]> statsMap = new HashMap<>();
        for (Object[] row : statsResult) {
            statsMap.put((Long) row[0], row);
        }

        List<ProgressReportResponse.EmployeeProgress> empProgressList = new ArrayList<>();
        for (User employee : allEmployees) {
            Object[] row = statsMap.get(employee.getId());
            long empTotal = (row != null) ? ((Number) row[3]).longValue() : 0;
            long empDone = (row != null) ? ((Number) row[4]).longValue() : 0;

            empProgressList.add(ProgressReportResponse.EmployeeProgress.builder()
                    .userId(employee.getId())
                    .fullName(employee.getFullName())
                    .avatar(employee.getAvatar())
                    .totalTasks(empTotal)
                    .doneTasks(empDone)
                    .completionPercentage(calculatePercentage(empDone, empTotal))
                    .build());
        }

        // Lấy thống kê 7 ngày gần nhất cho biểu đồ
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        List<Object[]> dailyResults = taskRepository.countCompletedTasksGroupByDay(tenantId, sevenDaysAgo);
        List<ProgressReportResponse.DailyStat> dailyStats = new ArrayList<>();
        for (Object[] row : dailyResults) {
            dailyStats.add(ProgressReportResponse.DailyStat.builder()
                    .label((String) row[0]) // YYYY-MM-DD
                    .value(((Number) row[1]).longValue())
                    .build());
        }

        // Lấy thống kê theo tháng (6 tháng gần nhất)
        java.time.LocalDateTime sixMonthsAgo = java.time.LocalDateTime.now().minusMonths(6);
        List<Object[]> monthlyResults = taskRepository.countCompletedTasksGroupByMonth(tenantId, sixMonthsAgo);
        List<ProgressReportResponse.DailyStat> monthlyStats = new ArrayList<>();
        for (Object[] row : monthlyResults) {
            monthlyStats.add(ProgressReportResponse.DailyStat.builder()
                    .label((String) row[0]) // YYYY-MM
                    .value(((Number) row[1]).longValue())
                    .build());
        }

        return ProgressReportResponse.builder()
                .totalTasks(total)
                .todoTasks(todo)
                .inProgressTasks(inProgress)
                .doneTasks(done)
                .completionPercentage(calculatePercentage(done, total))
                .totalEmployees(totalEmployees)
                .employeeProgresses(empProgressList)
                .dailyStats(dailyStats)
                .monthlyStats(monthlyStats) // Đính kèm dữ liệu theo tháng
                .build();
    }

    // Hàm tiện ích: Tính phần trăm an toàn (Tránh lỗi chia cho 0)
    private double calculatePercentage(long done, long total) {
        if (total == 0)
            return 0.0;
        double percentage = (double) done / total * 100;
        return Math.round(percentage * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân (VD: 85.5)
    }
}