package com.example.bezma.service;

import com.example.bezma.controller.NotificationController;
import com.example.bezma.dto.res.notification.NotificationResponse;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.service.iService.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final INotificationService notificationService;
    private final NotificationController notificationController;

    /**
     * Tạo notification và gửi real-time qua WebSocket
     */
    @Async
    public void publishNotification(Long userId, Long tenantId, String title, String message,
            NotificationType type, Long relatedTaskId) {
        try {
            // Thiết lập TenantContext cho luồng xử lý bất đồng bộ
            com.example.bezma.util.TenantContext.setCurrentTenantId(tenantId);

            // Lưu vào database và lấy object đã có ID
            NotificationResponse notification = notificationService.createNotification(userId, tenantId, title, message,
                    type, relatedTaskId);

            // Gửi real-time qua WebSocket với đầy đủ dữ liệu
            notificationController.sendNotificationToUser(userId, notification);
        } catch (Exception e) {
            // Log lỗi để dễ dàng gỡ lỗi
            System.err.println("Lỗi khi gửi thông báo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Xóa context sau khi hoàn thành
            com.example.bezma.util.TenantContext.clear();
        }
    }
}
