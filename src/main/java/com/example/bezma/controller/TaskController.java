package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.task.CreateTaskRequest;
import com.example.bezma.dto.res.task.TaskResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.service.impl.TaskServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Quản lý công việc và quy trình check-in/hoàn thành")
public class TaskController {

    private final TaskServiceImpl taskService;

    // ==========================================
    // NHÓM 1: QUERY (Lấy danh sách)
    // ==========================================

    @Operation(summary = "Lấy danh sách task của tôi")
    @GetMapping("/my-tasks")
    public ApiResponse<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<TaskResponse>>builder()
                .data(taskService.getMyTasks(currentUser.getId()))
                .message("Lấy danh sách task thành công!")
                .build();
    }

    @Operation(summary = "Lấy tất cả task của doanh nghiệp (Admin)")
    @GetMapping("/tenant/{tenantId}")
    public ApiResponse<List<TaskResponse>> getTasksByTenant(@PathVariable Long tenantId) {
        return ApiResponse.<List<TaskResponse>>builder()
                .data(taskService.getTasksByTenant(tenantId))
                .build();
    }

    @Operation(summary = "Lấy task chưa giao cho ai (Nhân viên tự nhận)")
    @GetMapping("/unassigned")
    public ApiResponse<List<TaskResponse>> getUnassignedTasks(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<TaskResponse>>builder()
                .data(taskService.getUnassignedTasks(currentUser.getTenant().getId()))
                .message("Lấy task chưa giao thành công!")
                .build();
    }

    // ==========================================
    // NHÓM 2: MUTATIONS (Tạo, cập nhật)
    // ==========================================

    @Operation(summary = "Tạo task mới")
    @PostMapping
    public ApiResponse<TaskResponse> createTask(@RequestBody CreateTaskRequest request) {
        return ApiResponse.<TaskResponse>builder()
                .data(taskService.createTask(request))
                .message("Tạo task thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật trạng thái task")
    @PutMapping("/{taskId}/status")
    public ApiResponse<TaskResponse> updateStatus(
            @PathVariable Long taskId,
            @RequestBody java.util.Map<String, String> body) {
        return ApiResponse.<TaskResponse>builder()
                .data(taskService.updateTaskStatus(taskId, body.get("status")))
                .build();
    }

    @Operation(summary = "Nhận task chưa giao (Nhân viên tự nhận)")
    @PostMapping("/{taskId}/claim")
    public ApiResponse<TaskResponse> claimTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<TaskResponse>builder()
                .data(taskService.claimTask(taskId, currentUser.getId()))
                .message("Nhận task thành công! Bạn có thể bắt đầu thực hiện.")
                .build();
    }

    // ==========================================
    // NHÓM 3: TASK WORKFLOW (Check-in → Complete)
    // ==========================================

    @Operation(summary = "Check-in tại hiện trường (GPS + ảnh + ghi chú)")
    @PostMapping(value = "/{taskId}/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaskResponse> checkInTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam("lat") BigDecimal lat,
            @RequestParam("lon") BigDecimal lon,
            @RequestParam(value = "note", required = false) String note) {

        return ApiResponse.<TaskResponse>builder()
                .data(taskService.checkInTask(taskId, currentUser.getId(), photo, lat, lon, note))
                .message("Check-in thành công! Bắt đầu thực hiện công việc.")
                .build();
    }

    @Operation(summary = "Hoàn thành task (ảnh hoàn thành + kết quả + xác nhận khách)")
    @PostMapping(value = "/{taskId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaskResponse> completeTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "resultNote", required = false) String resultNote,
            @RequestParam(value = "customerConfirmed", required = false) Boolean customerConfirmed) {

        return ApiResponse.<TaskResponse>builder()
                .data(taskService.completeTask(taskId, currentUser.getId(), photo, resultNote, customerConfirmed))
                .message("Nộp báo cáo hoàn thành! Đang chờ Admin phê duyệt.")
                .build();
    }

    @Operation(summary = "Upload báo cáo ảnh cho task")
    @PostMapping(value = "/{taskId}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadReport(
            @PathVariable Long taskId,
            @RequestParam("images") MultipartFile[] images) {
        return ApiResponse.<String>builder()
                .data(taskService.uploadReport(taskId, images))
                .build();
    }
}
