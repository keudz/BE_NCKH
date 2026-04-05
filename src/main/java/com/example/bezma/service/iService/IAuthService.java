package com.example.bezma.service.iService;

import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RegisterRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse loginWithZalo(ZaloLoginRequest request);
}
