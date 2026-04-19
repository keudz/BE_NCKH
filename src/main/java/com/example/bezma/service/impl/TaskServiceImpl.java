package com.example.bezma.service.impl;

import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.entity.task.Task;
import com.example.bezma.entity.task.TaskStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.NotificationPublisher;
import com.example.bezma.service.iService.ITaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
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
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
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

    private TaskResponse mapToResponse(Task task) {
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
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
