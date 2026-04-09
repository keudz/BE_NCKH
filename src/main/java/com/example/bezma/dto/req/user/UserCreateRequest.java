package com.example.bezma.dto.req.user;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String email;
    private String role;
}
