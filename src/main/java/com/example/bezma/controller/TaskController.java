package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.service.iService.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Các API quản lý công việc và báo cáo tiến độ")
public class TaskController {

    private final ITaskService taskService;

    @Operation(summary = "Lấy danh sách công việc của tôi (Nhân viên)")
    @GetMapping("/my-tasks")
    public ApiResponse<List<TaskResponse>> getMyTasks(@RequestAttribute("userId") Long userId) {
        return ApiResponse.<List<TaskResponse>>builder()
                .data(taskService.getMyTasks(userId))
                .build();
    }

    @Operation(summary = "Tạo công việc mới (Admin)")
    @PostMapping
    public ApiResponse<TaskResponse> createTask(@RequestBody CreateTaskRequest request) {
        return ApiResponse.<TaskResponse>builder()
                .data(taskService.createTask(request))
                .message("Tạo công việc thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật trạng thái công việc")
    @PutMapping("/{taskId}/status")
    public ApiResponse<TaskResponse> updateStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ApiResponse.<TaskResponse>builder()
                .data(taskService.updateTaskStatus(taskId, status))
                .message("Đã cập nhật trạng thái!")
                .build();
    }

    @Operation(summary = "Lấy toàn bộ công việc của doanh nghiệp (Admin)")
    @GetMapping("/tenant/{tenantId}")
    public ApiResponse<List<TaskResponse>> getByTenant(@PathVariable Long tenantId) {
        return ApiResponse.<List<TaskResponse>>builder()
                .data(taskService.getTasksByTenant(tenantId))
                .build();
    }

    @Operation(summary = "Báo cáo công việc (Upload ảnh)")
    @PostMapping("/{taskId}/report")
    public ApiResponse<String> uploadReport(
            @PathVariable Long taskId,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        return ApiResponse.<String>builder()
                .data(taskService.uploadReport(taskId, images))
                .message("Đã nộp báo cáo hình ảnh!")
                .build();
    }
}
