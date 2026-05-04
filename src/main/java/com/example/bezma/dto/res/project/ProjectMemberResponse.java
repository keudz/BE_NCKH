package com.example.bezma.dto.res.project;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectMemberResponse {
    private Long userId;
    private String fullName;
    private String roleName;
    private String phone;
}
