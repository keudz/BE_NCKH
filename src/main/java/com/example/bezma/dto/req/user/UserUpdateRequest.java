package com.example.bezma.dto.req.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private Long id;
    private String fullName;
    private String avatar;
    private String phone;
    private String email;
    private String birthday;
    private String gender;
    private String address;
    private String identityCard;
}
