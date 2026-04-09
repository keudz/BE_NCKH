package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.service.iService.IUserService; // Đổi import này
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Quản lý thông tin và tài khoản người dùng")
public class UserController {

    private final IUserService userService;

    @Operation(summary = "Lấy thông tin cá nhân của phiên đăng nhập hiện tại")
    @GetMapping("/profile")
    public ApiResponse<UserSummaryResponse> getProfile() {
        return ApiResponse.<UserSummaryResponse>builder()
                .data(userService.getMyProfile())
                .message("Lấy thông tin cá nhân thành công!")
                .build();
    }

    @Operation(summary = "Lấy danh sách toàn bộ nhân viên trong cùng doanh nghiệp")
    @GetMapping
    public ApiResponse<List<UserSummaryResponse>> getAllUsers() {
        return ApiResponse.<List<UserSummaryResponse>>builder()
                .data(userService.getAllUsersInMyTenant())
                .message("Lấy danh sách nhân viên thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật thông tin nhân viên")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserSummaryResponse> updateUser(
            @PathVariable(value = "id", required = false) Long id,
            @RequestBody UserUpdateRequest request) {

        return ApiResponse.<UserSummaryResponse>builder()
                .data(userService.updateUser(id, request))
                .message("Cập nhật thông tin nhân viên thành công!")
                .build();
    }

    @Operation(summary = "Xóa (ẩn) nhân viên khỏi hệ thống")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable("id") Long targetUserId) {

        userService.deleteUser(targetUserId);

        return ApiResponse.<Void>builder()
                .message("Xóa nhân viên thành công!")
                .build();
    }
}