package com.example.bezma.dto.res.user;

import lombok.Data;

import java.time.LocalDate;
//import java.util.List;

@Data
public class UserDetailResponse {
    private String fullName;
    private String avatarZalo; // URL ảnh từ Zalo hoặc Admin upload
    private String subPhone; // Số điện thoại phụ
    private LocalDate birthDate; // Dùng LocalDate để quản lý ngày sinh chuẩn

    // Thông tin tài chính & Marketing
    private String bankAccountNumber; // Số tài khoản ngân hàng
    private String bankName; // Tên ngân hàng (nên dùng Enum hoặc String)

    // (Tùy chọn) Ghi chú hoặc địa chỉ nếu sau này ông cần
    private String address;
}
