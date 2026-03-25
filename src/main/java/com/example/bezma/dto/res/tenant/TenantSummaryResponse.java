package com.example.bezma.dto.res.tenant;

import lombok.Data;

@Data
public class TenantSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private String logo;
}
