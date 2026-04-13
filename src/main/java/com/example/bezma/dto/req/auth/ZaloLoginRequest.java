package com.example.bezma.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZaloLoginRequest {
    @NotBlank(message = "Zalo Token không được để trống")
    private String zaloToken;

    @NotNull(message = "Tenant ID không được để trống")
    private Long tenantId;
}
