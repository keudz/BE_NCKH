package com.example.bezma.dto.req.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest {
    private Long userId;
    private String role;
}
