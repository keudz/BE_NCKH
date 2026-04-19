package com.example.bezma.service;

import com.example.bezma.controller.NotificationController;
import com.example.bezma.dto.res.notification.NotificationResponse;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.service.iService.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final INotificationService notificationService;
    private final NotificationController notificationController;

    /**
     * Tạo notification và gửi real-time qua WebSocket
     */
    public void publishNotification(Long userId, Long tenantId, String title, String message,
                                   NotificationType type, Long relatedTaskId) {
        // Lưu vào database
        notificationService.createNotification(userId, tenantId, title, message, type, relatedTaskId);

        // Gửi real-time qua WebSocket
        NotificationResponse notification = NotificationResponse.builder()
                .title(title)
                .message(message)
                .type(type)
                .relatedTaskId(relatedTaskId)
                .isRead(false)
                .build();

        notificationController.sendNotificationToUser(userId, notification);
    }
}

