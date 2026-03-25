package com.example.bezma.dto.req.tenant;

import lombok.Data;

@Data
public class TenantUpdateRequest {
    private String name;
    private String logo;
    private String description;
    private String domain;
}
