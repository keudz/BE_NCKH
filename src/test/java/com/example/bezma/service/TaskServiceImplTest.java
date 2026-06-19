package com.example.bezma.service;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.common.res.PageResponse;
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
import com.example.bezma.repository.ProjectRepository;
import com.example.bezma.repository.TaskRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.CloudinaryService;
import com.example.bezma.service.NotificationPublisher;
import com.example.bezma.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Unit Tests")
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Tenant testTenant;
    private User testUser;
    private User testAssignee;
    private Task testTask;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .name("Test Company")
                .tenantCode("TC001")
                .slug("test-company")
                .phone("0123456789")
                .email("company@test.com")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("admin")
                .password("pass")
                .fullName("Admin User")
                .tenant(testTenant)
                .build();

        testAssignee = User.builder()
                .id(2L)
                .username("staff01")
                .password("pass")
                .fullName("Nhân viên A")
                .tenant(testTenant)
                .build();

        testProject = Project.builder()
                .id(1L)
                .name("Dự án Alpha")
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Sửa chữa đường ống")
                .description("Sửa đường ống nước tại 123 Nguyễn Huệ")
                .priority("HIGH")
                .category("TECHNICAL")
                .status(TaskStatus.TO_DO)
                .assignee(testAssignee)
                .tenant(testTenant)
                .dueDate(LocalDateTime.of(2026, 6, 25, 17, 0))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== CREATE TASK TESTS ====================

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Tạo task mới có assignee → gửi notification")
        void createTask_WithAssignee_SendsNotification() {
            // Arrange
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("Lắp đặt điều hòa");
            request.setDescription("Lắp 2 máy điều hòa tại văn phòng KH");
            request.setPriority("HIGH");
            request.setCategory("TECHNICAL");
            request.setTenantId(1L);
            request.setAssigneeId(2L);
            request.setDueDate(LocalDateTime.of(2026, 6, 30, 17, 0));

            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testAssignee));

            Task savedTask = Task.builder()
                    .id(10L)
                    .title("Lắp đặt điều hòa")
                    .description("Lắp 2 máy điều hòa tại văn phòng KH")
                    .priority("HIGH")
                    .category("TECHNICAL")
                    .status(TaskStatus.TO_DO)
                    .assignee(testAssignee)
                    .tenant(testTenant)
                    .dueDate(LocalDateTime.of(2026, 6, 30, 17, 0))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

            // Act
            TaskResponse response = taskService.createTask(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Lắp đặt điều hòa");
            assertThat(response.getStatus()).isEqualTo("TO_DO");
            assertThat(response.getAssigneeId()).isEqualTo(2L);
            assertThat(response.getAssigneeName()).isEqualTo("Nhân viên A");

            verify(notificationPublisher).publishNotification(
                    eq(2L), eq(1L), anyString(), anyString(),
                    eq(NotificationType.TASK_ASSIGNED), eq(10L));
        }

        @Test
        @DisplayName("Tạo task không có assignee → không gửi notification")
        void createTask_WithoutAssignee_NoNotification() {
            // Arrange
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("Task chung");
            request.setDescription("Mô tả");
            request.setTenantId(1L);

            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));

            Task savedTask = Task.builder()
                    .id(11L).title("Task chung").status(TaskStatus.TO_DO)
                    .tenant(testTenant).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            // Act
            TaskResponse response = taskService.createTask(request);

            // Assert
            assertThat(response.getAssigneeName()).isEqualTo("Chưa giao");
            verify(notificationPublisher, never()).publishNotification(
                    anyLong(), anyLong(), anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("Tạo task tenant không tồn tại → throw TENANT_NOT_FOUND")
        void createTask_TenantNotFound_Throws() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTenantId(999L);
            when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> taskService.createTask(request), AppException.class);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TENANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Tạo task gắn project → response chứa projectId/Name")
        void createTask_WithProject_ReturnsProjectInfo() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("Task trong dự án");
            request.setTenantId(1L);
            request.setProjectId(1L);

            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            Task savedTask = Task.builder()
                    .id(12L).title("Task trong dự án").status(TaskStatus.TO_DO)
                    .tenant(testTenant).project(testProject)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            TaskResponse response = taskService.createTask(request);

            assertThat(response.getProjectId()).isEqualTo(1L);
            assertThat(response.getProjectName()).isEqualTo("Dự án Alpha");
        }
    }

    // ==================== UPDATE TASK STATUS TESTS ====================

    @Nested
    @DisplayName("Update Task Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Cập nhật status TO_DO → IN_PROGRESS thành công")
        void updateStatus_ToInProgress_Success() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            Task savedTask = Task.builder()
                    .id(1L).title(testTask.getTitle()).status(TaskStatus.IN_PROGRESS)
                    .assignee(testAssignee).tenant(testTenant)
                    .createdAt(testTask.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            try (var mocked = mockStatic(com.example.bezma.util.TenantContext.class)) {
                mocked.when(com.example.bezma.util.TenantContext::getCurrentTenantId).thenReturn(1L);

                TaskResponse response = taskService.updateTaskStatus(1L, "IN_PROGRESS");

                assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
            }
        }

        @Test
        @DisplayName("Cập nhật status task không tồn tại → throw exception")
        void updateStatus_TaskNotFound_Throws() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTaskStatus(999L, "DONE"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("Cập nhật status với string có khoảng trắng → normalize thành công")
        void updateStatus_NormalizesInput() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            Task saved = Task.builder()
                    .id(1L).title("test").status(TaskStatus.IN_PROGRESS)
                    .assignee(testAssignee).tenant(testTenant)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(saved);

            try (var mocked = mockStatic(com.example.bezma.util.TenantContext.class)) {
                mocked.when(com.example.bezma.util.TenantContext::getCurrentTenantId).thenReturn(1L);

                TaskResponse response = taskService.updateTaskStatus(1L, "in progress");
                assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
            }
        }
    }

    // ==================== CHECK-IN TASK TESTS ====================

    @Nested
    @DisplayName("Check-in Task Tests")
    class CheckInTaskTests {

        @Test
        @DisplayName("Check-in bởi đúng assignee → status = CHECKED_IN")
        void checkIn_ByAssignee_Success() {
            testTask.setStatus(TaskStatus.TO_DO);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            Task savedTask = Task.builder()
                    .id(1L).title(testTask.getTitle()).status(TaskStatus.CHECKED_IN)
                    .assignee(testAssignee).tenant(testTenant)
                    .checkInTime(LocalDateTime.now())
                    .checkInLatitude(BigDecimal.valueOf(10.762622))
                    .checkInLongitude(BigDecimal.valueOf(106.660172))
                    .createdAt(testTask.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);
            when(userRepository.findAdminsByTenantId(1L)).thenReturn(List.of(testUser));

            TaskResponse response = taskService.checkInTask(1L, 2L, null,
                    BigDecimal.valueOf(10.762622), BigDecimal.valueOf(106.660172), "Đã đến nơi");

            assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
            assertThat(response.getCheckInLatitude()).isNotNull();
        }

        @Test
        @DisplayName("Check-in bởi user không phải assignee → throw UNAUTHORIZED")
        void checkIn_NotAssignee_ThrowsUnauthorized() {
            testTask.setStatus(TaskStatus.TO_DO);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.checkInTask(1L, 99L, null,
                            BigDecimal.ZERO, BigDecimal.ZERO, null),
                    AppException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Check-in task đã DONE → throw INVALID_MESSAGE")
        void checkIn_AlreadyDone_ThrowsException() {
            testTask.setStatus(TaskStatus.DONE);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.checkInTask(1L, 2L, null,
                            BigDecimal.ZERO, BigDecimal.ZERO, null),
                    AppException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_MESSAGE);
        }

        @Test
        @DisplayName("Check-in task requirePhoto=true nhưng không có ảnh → throw")
        void checkIn_RequirePhotoNoPhoto_Throws() {
            testTask.setStatus(TaskStatus.TO_DO);
            testTask.setRequirePhoto(true);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.checkInTask(1L, 2L, null,
                            BigDecimal.ZERO, BigDecimal.ZERO, null),
                    AppException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_MESSAGE);
        }
    }

    // ==================== COMPLETE & APPROVE/REJECT TESTS ====================

    @Nested
    @DisplayName("Complete Task Tests")
    class CompleteTaskTests {

        @Test
        @DisplayName("Complete task CHECKED_IN → chuyển sang REVIEW")
        void completeTask_FromCheckedIn_StatusReview() {
            testTask.setStatus(TaskStatus.CHECKED_IN);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(userRepository.findAdminsByTenantId(1L)).thenReturn(List.of(testUser));

            Task savedTask = Task.builder()
                    .id(1L).title(testTask.getTitle()).status(TaskStatus.REVIEW)
                    .assignee(testAssignee).tenant(testTenant)
                    .completionTime(LocalDateTime.now())
                    .resultNote("Đã sửa xong")
                    .createdAt(testTask.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            TaskResponse response = taskService.completeTask(1L, 2L, null, "Đã sửa xong", true);

            assertThat(response.getStatus()).isEqualTo("REVIEW");
            verify(notificationPublisher, atLeast(1)).publishNotification(
                    anyLong(), anyLong(), anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("Complete bởi user không phải assignee → throw UNAUTHORIZED")
        void completeTask_NotAssignee_Throws() {
            testTask.setStatus(TaskStatus.CHECKED_IN);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.completeTask(1L, 99L, null, "note", true),
                    AppException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Approve & Reject Tests")
    class ApproveRejectTests {

        @Test
        @DisplayName("Approve task REVIEW → chuyển sang DONE, gửi notification")
        void approveTask_FromReview_StatusDone() {
            testTask.setStatus(TaskStatus.REVIEW);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            Task savedTask = Task.builder()
                    .id(1L).title(testTask.getTitle()).status(TaskStatus.DONE)
                    .assignee(testAssignee).tenant(testTenant)
                    .reviewNote("Tốt lắm!").reviewedBy(1L).reviewedAt(LocalDateTime.now())
                    .createdAt(testTask.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            TaskResponse response = taskService.approveTask(1L, 1L, "Tốt lắm!");

            assertThat(response.getStatus()).isEqualTo("DONE");
            assertThat(response.getReviewNote()).isEqualTo("Tốt lắm!");
            assertThat(response.getReviewedBy()).isEqualTo(1L);

            verify(notificationPublisher).publishNotification(
                    eq(2L), eq(1L), anyString(), anyString(),
                    eq(NotificationType.TASK_APPROVED), eq(1L));
        }

        @Test
        @DisplayName("Reject task REVIEW → chuyển sang REJECTED")
        void rejectTask_FromReview_StatusRejected() {
            testTask.setStatus(TaskStatus.REVIEW);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            Task savedTask = Task.builder()
                    .id(1L).title(testTask.getTitle()).status(TaskStatus.REJECTED)
                    .assignee(testAssignee).tenant(testTenant)
                    .reviewNote("Chưa đạt yêu cầu").reviewedBy(1L).reviewedAt(LocalDateTime.now())
                    .createdAt(testTask.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(savedTask);

            TaskResponse response = taskService.rejectTask(1L, 1L, "Chưa đạt yêu cầu");

            assertThat(response.getStatus()).isEqualTo("REJECTED");
            verify(notificationPublisher).publishNotification(
                    eq(2L), eq(1L), anyString(), contains("Chưa đạt yêu cầu"),
                    eq(NotificationType.TASK_REJECTED), eq(1L));
        }

        @Test
        @DisplayName("Approve task không ở trạng thái REVIEW → throw")
        void approveTask_NotInReview_Throws() {
            testTask.setStatus(TaskStatus.TO_DO);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.approveTask(1L, 1L, "note"), AppException.class);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_MESSAGE);
        }
    }

    // ==================== CLAIM TASK TESTS ====================

    @Nested
    @DisplayName("Claim Task Tests")
    class ClaimTaskTests {

        @Test
        @DisplayName("Nhân viên nhận task chưa giao → thành công")
        void claimTask_UnassignedTask_Success() {
            Task unassigned = Task.builder()
                    .id(5L).title("Task chưa giao").status(TaskStatus.TO_DO)
                    .tenant(testTenant).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByIdWithLock(5L)).thenReturn(Optional.of(unassigned));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testAssignee));
            when(userRepository.findAdminsByTenantId(1L)).thenReturn(List.of(testUser));

            Task saved = Task.builder()
                    .id(5L).title("Task chưa giao").status(TaskStatus.IN_PROGRESS)
                    .assignee(testAssignee).tenant(testTenant)
                    .createdAt(unassigned.getCreatedAt()).updatedAt(LocalDateTime.now())
                    .build();
            when(taskRepository.save(any())).thenReturn(saved);

            TaskResponse response = taskService.claimTask(5L, 2L);

            assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(response.getAssigneeId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Nhận task đã có assignee → throw INVALID_MESSAGE")
        void claimTask_AlreadyAssigned_Throws() {
            when(taskRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testTask));

            AppException ex = catchThrowableOfType(
                    () -> taskService.claimTask(1L, 2L), AppException.class);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_MESSAGE);
        }
    }

    // ==================== GET / QUERY TESTS ====================

    @Nested
    @DisplayName("Query Tasks Tests")
    class QueryTests {

        @Test
        @DisplayName("Lấy task theo tenant → phân trang đúng")
        void getTasksByTenant_Paged_ReturnsCorrectly() {
            Page<Task> page = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 1);
            when(taskRepository.findByTenantId(1L, PageRequest.of(0, 10))).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getTasksByTenant(1L, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Sửa chữa đường ống");
        }

        @Test
        @DisplayName("Lấy danh sách task của nhân viên")
        void getMyTasks_ReturnsAssigneeTasks() {
            when(taskRepository.findByAssigneeId(2L)).thenReturn(List.of(testTask));

            List<TaskResponse> tasks = taskService.getMyTasks(2L);

            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getAssigneeId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Lấy task chờ duyệt (REVIEW) → trả đúng status")
        void getPendingReviewTasks_ReturnsReviewOnly() {
            Task reviewTask = Task.builder()
                    .id(3L).title("Task chờ duyệt").status(TaskStatus.REVIEW)
                    .assignee(testAssignee).tenant(testTenant)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            Page<Task> page = new PageImpl<>(List.of(reviewTask), PageRequest.of(0, 10), 1);
            when(taskRepository.findByTenantIdAndStatus(1L, TaskStatus.REVIEW, PageRequest.of(0, 10)))
                    .thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getPendingReviewTasks(1L, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("REVIEW");
        }

        @Test
        @DisplayName("Tính duration (phút) từ checkIn đến completion")
        void mapToResponse_CalculatesDuration() {
            Task taskWithTimes = Task.builder()
                    .id(7L).title("Task hoàn thành").status(TaskStatus.DONE)
                    .assignee(testAssignee).tenant(testTenant)
                    .checkInTime(LocalDateTime.of(2026, 6, 19, 9, 0))
                    .completionTime(LocalDateTime.of(2026, 6, 19, 11, 30))
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(2L)).thenReturn(List.of(taskWithTimes));

            List<TaskResponse> tasks = taskService.getMyTasks(2L);

            assertThat(tasks.get(0).getDurationMinutes()).isEqualTo(150); // 2h30 = 150 phút
        }
    }

    // ==================== DELETE TASK TESTS ====================

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTests {

        @Test
        @DisplayName("Xóa task thành công")
        void deleteTask_Success() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            try (var mocked = mockStatic(com.example.bezma.util.TenantContext.class)) {
                mocked.when(com.example.bezma.util.TenantContext::getCurrentTenantId).thenReturn(1L);
                taskService.deleteTask(1L);
            }

            verify(taskRepository).delete(testTask);
        }

        @Test
        @DisplayName("Xóa task khác tenant → throw UNAUTHORIZED")
        void deleteTask_WrongTenant_Throws() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            try (var mocked = mockStatic(com.example.bezma.util.TenantContext.class)) {
                mocked.when(com.example.bezma.util.TenantContext::getCurrentTenantId).thenReturn(999L);

                AppException ex = catchThrowableOfType(
                        () -> taskService.deleteTask(1L), AppException.class);
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            }
        }
    }
}
