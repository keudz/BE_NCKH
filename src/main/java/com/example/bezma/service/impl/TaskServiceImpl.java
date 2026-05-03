package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.entity.project.Project;
import com.example.bezma.entity.task.Task;
import com.example.bezma.entity.task.TaskStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.CustomerRepository;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.NotificationPublisher;
import com.example.bezma.service.iService.ITaskService;
import com.example.bezma.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import com.example.bezma.common.res.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements ITaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final com.example.bezma.repository.ProjectRepository projectRepository;
    private final NotificationPublisher notificationPublisher;
    private final CloudinaryService cloudinaryService;

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

        com.example.bezma.entity.project.Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
        }

        com.example.bezma.entity.customer.Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        if (customer == null && request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            customer = customerRepository.findByTenantIdAndPhoneNumber(tenant.getId(), request.getPhoneNumber())
                    .orElse(null);
        }

        if (customer == null && request.getCustomerName() != null && !request.getCustomerName().isEmpty()) {
            customer = new com.example.bezma.entity.customer.Customer();
            customer.setTenant(tenant);
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        if (customer != null) {
            // Cập nhật hoặc điền thông tin mới nhất từ yêu cầu
            if (request.getCustomerName() != null)
                customer.setName(request.getCustomerName());
            if (request.getCompanyName() != null)
                customer.setCompanyName(request.getCompanyName());
            if (request.getAddress() != null)
                customer.setAddress(request.getAddress());

            customer = customerRepository.save(customer);
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
                .customer(customer)
                .project(project)

                .customerName(customer != null ? customer.getName() : request.getCustomerName())
                .phoneNumber(customer != null ? customer.getPhoneNumber() : request.getPhoneNumber())
                .address(customer != null ? customer.getAddress() : request.getAddress())
                .companyName(customer != null ? customer.getCompanyName() : request.getCompanyName())
                .estimatedPrice(request.getEstimatedPrice())
                .requirePhoto(request.getRequirePhoto() != null ? request.getRequirePhoto() : false)
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
                    savedTask.getId());
        }

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, CreateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        Long currentTenantId = com.example.bezma.util.TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !task.getTenant().getId().equals(currentTenantId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        // Cập nhật thông tin khách hàng (nếu có)
        task.setCustomerName(request.getCustomerName());
        task.setCompanyName(request.getCompanyName());
        task.setAddress(request.getAddress());
        task.setPhoneNumber(request.getPhoneNumber());
        task.setEstimatedPrice(request.getEstimatedPrice());

        Task savedTask = taskRepository.save(task);

        // Gửi thông báo nếu có người nhận việc mới
        Long newAssigneeId = request.getAssigneeId();
        if (newAssigneeId != null && !newAssigneeId.equals(oldAssigneeId)) {
            notificationPublisher.publishNotification(
                    newAssigneeId,
                    task.getTenant().getId(),
                    "Công việc mới: " + task.getTitle(),
                    "Bạn được giao nhiệm vụ: " + task.getDescription(),
                    NotificationType.TASK_ASSIGNED,
                    savedTask.getId());
        }

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        Long currentTenantId = com.example.bezma.util.TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !task.getTenant().getId().equals(currentTenantId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chuyển đổi string sang Enum (xử lý cả trường hợp gạch ngang/dấu cách nếu có)
        String normalizedStatus = status.toUpperCase().replace(" ", "_");
        task.setStatus(TaskStatus.valueOf(normalizedStatus));

        return mapToResponse(taskRepository.save(task));
    }

    @Override
    public PageResponse<TaskResponse> getTasksByTenant(Long tenantId, int page, int size) {
        Page<Task> taskPage = taskRepository.findByTenantId(tenantId, PageRequest.of(page, size));
        List<TaskResponse> content = taskPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(content)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Override
    public List<TaskResponse> getTasksByTenant(Long tenantId) {
        return taskRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse checkInTask(Long taskId, Long userId,
            MultipartFile photo,
            BigDecimal latitude, BigDecimal longitude,
            String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        // 1. Validate: chỉ assignee mới được check-in
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Validate: Trạng thái hợp lệ (Chưa làm hoặc đang làm)
        if (task.getStatus() != TaskStatus.TO_DO && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        // 3. Geofencing: Tạm thời tắt kiểm tra khoảng cách để dễ dàng Testing
        /*
         * Tenant tenant = task.getTenant();
         * if (tenant.getOfficeLatitude() != null && tenant.getOfficeLongitude() !=
         * null) {
         * double distance = calculateDistance(
         * latitude.doubleValue(), longitude.doubleValue(),
         * tenant.getOfficeLatitude().doubleValue(),
         * tenant.getOfficeLongitude().doubleValue());
         * 
         * double allowedRadius = tenant.getAllowedRadius() != null ?
         * tenant.getAllowedRadius() : 500.0;
         * if (distance > allowedRadius) {
         * throw new AppException(ErrorCode.TASK_LOCATION_OUT_OF_RANGE);
         * }
         * }
         */

        // Lưu ảnh hiện trường
        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoUrl = cloudinaryService.uploadFile(photo, "tasks/checkin");
            } catch (Exception e) {
                log.error("Lỗi lưu ảnh check-in: {}", e.getMessage());
            }
        }

        // Validate: Nếu task yêu cầu ảnh bắt buộc
        if (Boolean.TRUE.equals(task.getRequirePhoto()) && (photoUrl == null)) {
            throw new AppException(ErrorCode.INVALID_MESSAGE); // "Bạn cần chụp ảnh minh chứng để check-in"
        }

        // Cập nhật task
        task.setStatus(TaskStatus.CHECKED_IN);
        task.setCheckInTime(LocalDateTime.now());
        // task.setCheckInLatitude(latitude);
        // task.setCheckInLongitude(longitude);
        task.setCheckInPhoto(photoUrl);
        task.setCheckInNote(note);

        return mapToResponse(taskRepository.save(task));
    }

    // private double calculateDistance(double lat1, double lon1, double lat2,
    // double lon2) {
    // final int R = 6371; // Bán kính Trái đất (km)
    // double latDistance = Math.toRadians(lat2 - lat1);
    // double lonDistance = Math.toRadians(lon2 - lon1);
    // double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
    // + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
    // * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    // double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    // return R * c * 1000; // Trả về mét
    // }

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
                photoUrl = cloudinaryService.uploadFile(photo, "tasks/completion");
            } catch (Exception e) {
                log.error("Lỗi lưu ảnh completion: {}", e.getMessage());
            }
        }

        // Validate: Nếu task yêu cầu ảnh bắt buộc
        if (Boolean.TRUE.equals(task.getRequirePhoto()) && (photoUrl == null)) {
            throw new AppException(ErrorCode.INVALID_MESSAGE); // "Bạn cần chụp ảnh minh chứng khi hoàn thành"
        }

        // Cập nhật task → chuyển sang REVIEW để Admin duyệt
        task.setStatus(TaskStatus.REVIEW);
        task.setCompletionPhoto(photoUrl);
        task.setCompletionTime(LocalDateTime.now());
        task.setResultNote(resultNote);
        task.setCustomerConfirmed(customerConfirmed != null ? customerConfirmed : false);

        return mapToResponse(taskRepository.save(task));
    }

    public List<TaskResponse> getUnassignedTasks(Long tenantId) {
        return taskRepository.findByTenantIdAndAssigneeIsNull(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse claimTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdWithLock(taskId)
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
                    String url = cloudinaryService.uploadFile(image, "tasks");
                    imageUrls.add(url);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi lưu file: " + e.getMessage());
                }
            }
            task.setReportImages(String.join(",", imageUrls));
        }

        taskRepository.save(task);
        return "Đã nộp báo cáo " + (images != null ? images.length : 0) + " ảnh!";
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        Long currentTenantId = com.example.bezma.util.TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !task.getTenant().getId().equals(currentTenantId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        taskRepository.delete(task);
    }

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
                .status(task.getStatus() != null ? task.getStatus().name() : "TO_DO")
                .dueDate(task.getDueDate())
                .date(task.getDueDate() != null ? task.getDueDate().toString().split("T")[0] : null)
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
                // Customer Info
                .customerName(task.getCustomerName())
                .phoneNumber(task.getPhoneNumber())
                .address(task.getAddress())
                .companyName(task.getCompanyName())
                .estimatedPrice(task.getEstimatedPrice())
                // Project info
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                // Review info
                .reviewNote(task.getReviewNote())
                .reviewedBy(task.getReviewedBy())
                .reviewedAt(task.getReviewedAt())
                .requirePhoto(task.getRequirePhoto())
                .build();
    }

    @Override
    @Transactional
    public TaskResponse approveTask(Long taskId, Long adminId, String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        task.setStatus(TaskStatus.DONE);
        task.setReviewNote(note);
        task.setReviewedBy(adminId);
        task.setReviewedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        // Gửi notification cho nhân viên
        if (task.getAssignee() != null) {
            notificationPublisher.publishNotification(
                    task.getAssignee().getId(),
                    task.getTenant().getId(),
                    "✅ Công việc đã hoàn thành",
                    "Công việc '" + task.getTitle() + "' đã được Admin phê duyệt.",
                    NotificationType.TASK_APPROVED,
                    task.getId());
        }

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse rejectTask(Long taskId, Long adminId, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        task.setStatus(TaskStatus.REJECTED);
        task.setReviewNote(reason);
        task.setReviewedBy(adminId);
        task.setReviewedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        // Gửi notification cho nhân viên
        if (task.getAssignee() != null) {
            notificationPublisher.publishNotification(
                    task.getAssignee().getId(),
                    task.getTenant().getId(),
                    "❌ Công việc bị từ chối",
                    "Công việc '" + task.getTitle() + "' bị từ chối. Lý do: " + reason,
                    NotificationType.TASK_REJECTED,
                    task.getId());
        }

        return mapToResponse(savedTask);
    }

    @Override
    public PageResponse<TaskResponse> getPendingReviewTasks(Long tenantId, int page, int size) {
        Page<Task> taskPage = taskRepository.findByTenantIdAndStatus(tenantId, TaskStatus.REVIEW, PageRequest.of(page, size));
        List<TaskResponse> content = taskPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(content)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Override
    public List<TaskResponse> getPendingReviewTasks(Long tenantId) {
        return taskRepository.findByTenantIdAndStatus(tenantId, TaskStatus.REVIEW).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
