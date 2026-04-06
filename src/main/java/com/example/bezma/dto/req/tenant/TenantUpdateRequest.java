package com.example.bezma.dto.req.tenant;

import com.example.bezma.validation.UniqueDomain;
import com.example.bezma.validation.UniqueTenantCode;
import lombok.Data;
@Data
public class TenantUpdateRequest {
    private String name;
    private String logo;
    private String description;
    
    @UniqueDomain
    private String domain;
    
    @UniqueTenantCode
    private String tenantCode;
}
