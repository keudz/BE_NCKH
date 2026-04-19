package com.example.bezma.dto.res.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicResponse {

    private Long id;

    private String username;

    private String fullName;

    private String avatar;

    private String email;

    private String phone;
}
