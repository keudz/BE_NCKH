package com.example.bezma.service.iService;

import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    AuthResponse loginZalo(ZaloLoginRequest request);
    
    void requestTwoStepReset(String emailOrPhone);
    void confirmTwoStepReset(String emailOrPhone, String companyOtp, String adminOtp, String newPassword);
}
