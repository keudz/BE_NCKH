package com.example.bezma.service.iService;

import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest; // Thêm dòng này
import com.example.bezma.dto.res.auth.AuthResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request); // Thêm dòng này
}