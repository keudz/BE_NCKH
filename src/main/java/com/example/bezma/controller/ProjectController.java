package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.project.CreateProjectRequest;
import com.example.bezma.dto.res.project.ProjectResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.service.impl.ProjectServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project Management", description = "Quản lý dự án")
public class ProjectController {

    private final ProjectServiceImpl projectService;

    @Operation(summary = "Tạo dự án mới (Admin)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<ProjectResponse>builder()
                .data(projectService.createProject(request, currentUser.getTenant().getId()))
                .message("Tạo dự án thành công!")
                .build();
    }

    @Operation(summary = "Lấy danh sách dự án của tenant")
    @GetMapping
    public ApiResponse<List<ProjectResponse>> getProjects(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .data(projectService.getProjectsByTenant(currentUser.getTenant().getId()))
                .build();
    }

    @Operation(summary = "Lấy danh sách dự án do tôi quản lý (Manager)")
    @GetMapping("/my-managed")
    public ApiResponse<List<ProjectResponse>> getMyManagedProjects(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<ProjectResponse>>builder()
                .data(projectService.getProjectsByManager(currentUser.getId()))
                .build();
    }

    @Operation(summary = "Xem chi tiết dự án")
    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getProjectDetail(@PathVariable Long id) {
        return ApiResponse.<ProjectResponse>builder()
                .data(projectService.getProjectDetail(id))
                .build();
    }

    @Operation(summary = "Thêm thành viên vào dự án (Admin/Manager)")
    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ApiResponse<Void> addMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.addMemberToProject(id, userId);
        return ApiResponse.<Void>builder().message("Thêm thành viên thành công!").build();
    }

    @Operation(summary = "Xóa thành viên khỏi dự án (Admin/Manager)")
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMemberFromProject(id, userId);
        return ApiResponse.<Void>builder().message("Xóa thành viên thành công!").build();
    }
}
