package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.entity.task.Task;
import com.example.bezma.entity.task.TaskStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.NotificationPublisher;
import com.example.bezma.service.iService.ITaskService;
import com.example.bezma.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements ITaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public List<TaskResponse> getMyTasks(Long userId) {
        return taskRepository.findByAssigneeId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .category(request.getCategory())
                .dueDate(request.getDueDate())
                .status(TaskStatus.TO_DO)
                .assignee(assignee)
                .tenant(tenant)
                .build();

        Task savedTask = taskRepository.save(task);

        // Gửi notification nếu có người được giao nhiệm vụ
        if (assignee != null) {
            notificationPublisher.publishNotification(
                    assignee.getId(),
                    tenant.getId(),
                    "Công việc mới: " + task.getTitle(),
                    "Bạn được giao nhiệm vụ: " + task.getDescription(),
                    NotificationType.TASK_ASSIGNED,
                    savedTask.getId()
            );
        }

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
        
        // Chuyển đổi string sang Enum (xử lý cả trường hợp gạch ngang/dấu cách nếu có)
        String normalizedStatus = status.toUpperCase().replace(" ", "_");
        task.setStatus(TaskStatus.valueOf(normalizedStatus));
        
        return mapToResponse(taskRepository.save(task));
    }

    @Override
    public List<TaskResponse> getTasksByTenant(Long tenantId) {
        return taskRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // TASK CHECK-IN: Bắt đầu thực hiện tại hiện trường
    // ==========================================

    @Transactional
    public TaskResponse checkInTask(Long taskId, Long userId,
                                     MultipartFile photo,
                                     BigDecimal latitude, BigDecimal longitude,
                                     String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        // Validate: chỉ assignee mới được check-in
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate: chỉ task IN_PROGRESS mới check-in được
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        // Lưu ảnh hiện trường
        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoUrl = FileUtils.saveFile("uploads/tasks/checkin", photo);
            } catch (IOException e) {
                log.error("Lỗi lưu ảnh check-in: {}", e.getMessage());
            }
        }

        // Cập nhật task
        task.setStatus(TaskStatus.CHECKED_IN);
        task.setCheckInTime(LocalDateTime.now());
        task.setCheckInLatitude(latitude);
        task.setCheckInLongitude(longitude);
        task.setCheckInPhoto(photoUrl);
        task.setCheckInNote(note);

        return mapToResponse(taskRepository.save(task));
    }

    // ==========================================
    // TASK COMPLETE: Hoàn thành công việc
    // ==========================================

    @Transactional
    public TaskResponse completeTask(Long taskId, Long userId,
                                      MultipartFile photo,
                                      String resultNote,
                                      Boolean customerConfirmed) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        // Validate: chỉ assignee mới được complete
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate: chỉ task CHECKED_IN hoặc IN_PROGRESS mới complete được
        if (task.getStatus() != TaskStatus.CHECKED_IN && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        // Lưu ảnh hoàn thành
        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoUrl = FileUtils.saveFile("uploads/tasks/completion", photo);
            } catch (IOException e) {
                log.error("Lỗi lưu ảnh completion: {}", e.getMessage());
            }
        }

        // Cập nhật task → chuyển sang REVIEW để Admin duyệt
        task.setStatus(TaskStatus.REVIEW);
        task.setCompletionPhoto(photoUrl);
        task.setCompletionTime(LocalDateTime.now());
        task.setResultNote(resultNote);
        task.setCustomerConfirmed(customerConfirmed != null ? customerConfirmed : false);

        return mapToResponse(taskRepository.save(task));
    }

    // ==========================================
    // LẤY TASK CHƯA GIAO (Cho nhân viên tự nhận)
    // ==========================================

    public List<TaskResponse> getUnassignedTasks(Long tenantId) {
        return taskRepository.findByTenantIdAndAssigneeIsNull(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // NHẬN TASK (Nhân viên tự nhận task chưa giao)
    // ==========================================

    @Transactional
    public TaskResponse claimTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        // Chỉ nhận task chưa giao cho ai
        if (task.getAssignee() != null) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        task.setAssignee(user);
        task.setStatus(TaskStatus.IN_PROGRESS);

        return mapToResponse(taskRepository.save(task));
    }

    // ==========================================
    // REPORT UPLOAD (Giữ lại logic cũ)
    // ==========================================

    @Override
    @Transactional
    public String uploadReport(Long taskId, MultipartFile[] images) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
        
        // Cập nhật trạng thái công việc thành REVIEW khi nộp báo cáo
        task.setStatus(TaskStatus.REVIEW);
        
        if (images != null && images.length > 0) {
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            for (MultipartFile image : images) {
                try {
                    String url = FileUtils.saveFile("uploads/tasks", image);
                    imageUrls.add(url);
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi lưu file: " + e.getMessage());
                }
            }
            task.setReportImages(String.join(",", imageUrls));
        }
        
        taskRepository.save(task);
        
        return "Đã nộp báo cáo " + (images != null ? images.length : 0) + " ảnh!";
    }

    // ==========================================
    // MAPPER
    // ==========================================

    private TaskResponse mapToResponse(Task task) {
        // Tính thời gian thực hiện (phút)
        Long durationMinutes = null;
        if (task.getCheckInTime() != null && task.getCompletionTime() != null) {
            durationMinutes = Duration.between(task.getCheckInTime(), task.getCompletionTime()).toMinutes();
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .priority(task.getPriority())
                .status(task.getStatus().name().replace("_", " "))
                .dueDate(task.getDueDate())
                .date(task.getDueDate() != null ? task.getDueDate().toString().substring(0, 10) : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : "Chưa giao")
                .reportImages(task.getReportImages())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                // Check-in fields
                .checkInTime(task.getCheckInTime())
                .checkInLatitude(task.getCheckInLatitude())
                .checkInLongitude(task.getCheckInLongitude())
                .checkInPhoto(task.getCheckInPhoto())
                .checkInNote(task.getCheckInNote())
                // Completion fields
                .completionPhoto(task.getCompletionPhoto())
                .completionTime(task.getCompletionTime())
                .resultNote(task.getResultNote())
                .customerConfirmed(task.getCustomerConfirmed())
                .durationMinutes(durationMinutes)
                .build();
    }
}
