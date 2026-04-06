package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.service.iService.IUserService; // Đổi import này
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}