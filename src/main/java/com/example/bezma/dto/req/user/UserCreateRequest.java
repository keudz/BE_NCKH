package com.example.bezma.dto.req.user;

import lombok.Data;
import java.util.List;

@Data
public class UserCreateRequest {
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private String password;
    private List<String> roles;
    private String status;
    private String birthday;
    private String gender;
    private String address;
    private String identityCard;
}
