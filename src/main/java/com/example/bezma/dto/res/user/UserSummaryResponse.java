package com.example.bezma.dto.res.user;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class UserSummaryResponse {
    private Long id;
    private String fullName;
    private String username;
    private String slug;
    private Long tenantId;
    private String tenantCode;
    private String phone;
    private String email;
    private Boolean isActive;
    private String roleName;
    private String avatar;
}
