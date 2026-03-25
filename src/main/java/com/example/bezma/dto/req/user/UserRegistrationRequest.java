package com.example.bezma.dto.req.user;

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private String position;
    private String teamName;
    private String role;
}
