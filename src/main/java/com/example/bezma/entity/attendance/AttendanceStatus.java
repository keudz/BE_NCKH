package com.example.bezma.entity.attendance;

public enum AttendanceStatus {
    SUCCESS,        // Chấm công thành công (Chung)
    ON_TIME,        // Đúng giờ
    LATE,           // Đi muộn
    CHECK_OUT,      // Về đúng giờ
    EARLY_LEAVE,    // Về sớm
    FAIL_FACE,      // Không khớp khuôn mặt
    FAIL_LOCATION,  // Sai vị trí GPS
    PENDING         // Đang chờ xử lý (nếu có hậu kiểm)
}
