package com.example.bezma.entity.task;

public enum TaskStatus {
    TO_DO,          // Mới tạo, chưa giao hoặc chưa bắt đầu
    IN_PROGRESS,    // Đã nhận việc nhưng chưa check-in hiện trường
    CHECKED_IN,     // Đã check-in (GPS + ảnh hiện trường) — đang thực hiện
    REVIEW,         // Nhân viên đã nộp báo cáo hoàn thành, chờ Admin duyệt
    REJECTED,       // Admin từ chối báo cáo, yêu cầu làm lại
    DONE            // Admin đã phê duyệt — hoàn tất
}
