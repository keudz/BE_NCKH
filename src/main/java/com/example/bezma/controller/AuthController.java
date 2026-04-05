package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RegisterRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.service.iService.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login/basic")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .data(authService.login(request))
                .message("Đăng nhập thành công!")
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .data(authService.register(request))
                .message("Đăng ký thành công!")
                .build();
    }

    @PostMapping("/login/zalo")
    public ApiResponse<AuthResponse> zaloLogin(@RequestBody ZaloLoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .data(authService.loginWithZalo(request))
                .message("Đăng nhập bằng Zalo thành công!")
                .build();
    }
}
