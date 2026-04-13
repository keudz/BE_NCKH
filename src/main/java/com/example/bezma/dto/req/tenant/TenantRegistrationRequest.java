package com.example.bezma.dto.req.tenant;

import com.example.bezma.entity.tenant.PlanType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRegistrationRequest {
    @NotBlank(message = "Tên doanh nghiệp không được để trống")
    private String name;
    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    private String logo;
    private String description;
    private PlanType planType;
}
