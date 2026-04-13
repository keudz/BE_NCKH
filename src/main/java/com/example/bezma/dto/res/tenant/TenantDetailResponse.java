package com.example.bezma.dto.res.tenant;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantDetailResponse {
    private Long id;
    private String name;
    private String tenantCode;
    private String slug;
    private String domain;
    private String email;
    private String logo;
    private String description;
    private boolean active;
    private String planType;
    private String statusConfirm;
    private LocalDateTime createdAt;
}
