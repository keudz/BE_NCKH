package com.example.bezma.dto.req.tenant;

import com.example.bezma.entity.tenant.PlanType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRegistrationRequest {
    // --- Business Info ---
    @NotBlank(message = "Tên doanh nghiệp không được để trống")
    private String name;

    @NotBlank(message = "Email doanh nghiệp không được để trống")
    @Email(message = "Email doanh nghiệp không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại doanh nghiệp không được để trống")
    private String phone;

    private String taxCode;

    @NotBlank(message = "Loại hình doanh nghiệp không được để trống")
    private String businessType;

    // --- Granular Address ---
    private String province;
    private String district;
    private String ward;
    private String street;

    // --- Admin User Info (Người đại diện) ---
    @NotBlank(message = "Tên người đại diện không được để trống")
    private String adminFullName;

    @NotBlank(message = "Email người đại diện không được để trống")
    @Email(message = "Email cá nhân không đúng định dạng")
    private String adminEmail;

    @NotBlank(message = "SĐT người đại diện không được để trống")
    private String adminPhone;

    private String logo;
    private String description;
    private PlanType planType;
}
