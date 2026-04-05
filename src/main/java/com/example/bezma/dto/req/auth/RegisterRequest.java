package com.example.bezma.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    @Pattern(regexp = "^(0|84)(3|5|7|8|9)([0-9]{8})$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    private String zaloId;
    private String fullName;
    private String role;
}
