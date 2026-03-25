package com.example.bezma.entity.tenant;

public enum RegistrationStatus {
    VERIFIED,
    PENDING_VERIFICATION, // Chờ bấm link email
    ACTIVE,               // Đã xác thực và hoạt động
    SUSPENDED             // Bị khóa
}
