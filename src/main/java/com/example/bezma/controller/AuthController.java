package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.service.iService.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API xác thực và cấp quyền người dùng")
public class AuthController {

    private final IAuthService authService;

    @Operation(summary = "Đăng nhập bằng Username & Password (Basic Login)")
    @PostMapping("/login/basic")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .data(data)
                .message("Đăng nhập thành công!")
                .build();
    }

    @Operation(summary = "Làm mới Token (Refresh Token)")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .data(authService.refreshToken(request))
                .message("Làm mới token thành công!")
                .build();
    }



    @Operation(summary = "Đăng xuất tài khoản (Xóa Cookie)")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {


        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ApiResponse.<Void>builder()
                .message("Đăng xuất thành công!")
                .build();
    }

    @Operation(summary = "Đăng nhập bằng Zalo")
    @PostMapping("/login/zalo")
    public ApiResponse<AuthResponse> loginZalo(
            @RequestBody @Valid ZaloLoginRequest request,
            HttpServletResponse response) {
        AuthResponse data = authService.loginZalo(request);


        ResponseCookie cookie = ResponseCookie.from("access_token", data.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60) // Sống 1 ngày
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<AuthResponse>builder()
                .data(data)
                .message("Đăng nhập Zalo thành công!")
                .build();
    }
}