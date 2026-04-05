package com.example.bezma.dto.req.auth;

import lombok.Data;

@Data
public class ZaloLoginRequest {
    private String zaloId;
    private String fullName;
    private String avatar;
    private String phone;
}
