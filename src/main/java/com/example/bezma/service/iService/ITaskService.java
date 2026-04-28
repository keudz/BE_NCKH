package com.example.bezma.service.iService;

import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import java.util.List;

public interface ITaskService {
    List<TaskResponse> getMyTasks(Long userId);
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTaskStatus(Long taskId, String status);
    List<TaskResponse> getTasksByTenant(Long tenantId);
    List<TaskResponse> getTasksByProject(Long projectId);
    TaskResponse updateTask(Long taskId, CreateTaskRequest request);
    String uploadReport(Long taskId, org.springframework.web.multipart.MultipartFile[] images);
    void deleteTask(Long taskId);
}
