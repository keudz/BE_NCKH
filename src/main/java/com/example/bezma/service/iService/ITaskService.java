package com.example.bezma.service.iService;

import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.common.res.PageResponse;
import java.util.List;

public interface ITaskService {
    List<TaskResponse> getMyTasks(Long userId);
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTaskStatus(Long taskId, String status);
    PageResponse<TaskResponse> getTasksByTenant(Long tenantId, int page, int size);
    List<TaskResponse> getTasksByTenant(Long tenantId);
    List<TaskResponse> getTasksByProject(Long projectId);
    TaskResponse updateTask(Long taskId, CreateTaskRequest request);
    String uploadReport(Long taskId, org.springframework.web.multipart.MultipartFile[] images);
    void deleteTask(Long taskId);

    // Review & Approval
    TaskResponse approveTask(Long taskId, Long adminId, String note);
    TaskResponse rejectTask(Long taskId, Long adminId, String reason);
    PageResponse<TaskResponse> getPendingReviewTasks(Long tenantId, int page, int size);
    List<TaskResponse> getPendingReviewTasks(Long tenantId);
}
