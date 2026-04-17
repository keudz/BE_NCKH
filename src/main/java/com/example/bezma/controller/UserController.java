package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.service.iService.IUserService;
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

    @Operation(summary = "Tạo nhân viên mới (Dành cho Admin)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserSummaryResponse> createUser(@RequestBody UserCreateRequest request) {
        return ApiResponse.<UserSummaryResponse>builder()
                .data(userService.createUser(request))
                .message("Tạo nhân viên thành công!")
                .build();
    }

    @Operation(summary = "Lấy thông tin cá nhân của phiên đăng nhập hiện tại")
    @GetMapping("/profile")
    public ApiResponse<UserSummaryResponse> getProfile() {
        return ApiResponse.<UserSummaryResponse>builder()
                .data(userService.getMyProfile())
                .message("Lấy thông tin cá nhân thành công!")
                .build();
    }

    @Operation(summary = "Lấy danh sách nhân viên (0: Đang làm việc, 1: Đã xóa)")
    @GetMapping
    public ApiResponse<List<UserSummaryResponse>> getAllUsers(
            @RequestParam(value = "isDeleted", required = false, defaultValue = "false") Boolean isDeleted) {
        return ApiResponse.<List<UserSummaryResponse>>builder()
                .data(userService.getAllUsersInMyTenant(isDeleted))
                .message("Lấy danh sách nhân viên thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật thông tin nhân viên")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable("id") Long targetUserId) {

        userService.deleteUser(targetUserId);

        return ApiResponse.<Void>builder()
                .message("Xóa nhân viên thành công!")
                .build();
    }

    @Operation(summary = "Khôi phục nhân viên từ thùng rác")
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> restoreUser(@PathVariable("id") Long targetUserId) {
        userService.restoreUser(targetUserId);
        return ApiResponse.<Void>builder()
                .message("Khôi phục nhân viên thành công!")
                .build();
    }
}