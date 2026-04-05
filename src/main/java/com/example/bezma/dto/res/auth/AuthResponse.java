package com.example.bezma.dto.res.auth;

import lombok.Data;
import com.example.bezma.dto.res.user.UserDetailResponse;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserDetailResponse user;
}
