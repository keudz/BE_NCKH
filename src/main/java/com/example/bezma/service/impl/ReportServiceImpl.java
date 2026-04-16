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
        if(!adminOptional.isPresent()){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        User admin = adminOptional.get();
        Long tenantId = admin.getTenant().getId();

        // Thống kê tổng công ty
        long total = taskRepository.countByTenantId(tenantId);
        long todo = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.TO_DO);
        long inProgress = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.IN_PROGRESS);
        long done = taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.DONE);

        // Lấy danh sách tiến độ từng nhân viên
        List<User> employees = userRepository.findAllByTenantId(tenantId);
        List<ProgressReportResponse.EmployeeProgress> empProgressList = new ArrayList<>();

        for (User emp : employees) {
            long empTotal = taskRepository.countByAssigneeId(emp.getId());
            long empDone = taskRepository.countByAssigneeIdAndStatus(emp.getId(), TaskStatus.DONE);

            empProgressList.add(ProgressReportResponse.EmployeeProgress.builder()
                    .userId(emp.getId())
                    .fullName(emp.getFullName())
                    .avatar(emp.getAvatar())
                    .totalTasks(empTotal)
                    .doneTasks(empDone)
                    .completionPercentage(calculatePercentage(empDone, empTotal))
                    .build());
        }

        return ProgressReportResponse.builder()
                .totalTasks(total)
                .todoTasks(todo)
                .inProgressTasks(inProgress)
                .doneTasks(done)
                .completionPercentage(calculatePercentage(done, total))
                .employeeProgresses(empProgressList) // Đính kèm list nhân viên vào cho Admin xem
                .build();
    }

    // Hàm tiện ích: Tính phần trăm an toàn (Tránh lỗi chia cho 0)
    private double calculatePercentage(long done, long total) {
        if (total == 0) return 0.0;
        double percentage = (double) done / total * 100;
        return Math.round(percentage * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân (VD: 85.5)
    }
}