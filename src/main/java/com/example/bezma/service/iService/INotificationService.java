package com.example.bezma.service.iService;

import com.example.bezma.dto.res.notification.NotificationResponse;
import com.example.bezma.entity.notification.NotificationType;

import java.util.List;

public interface INotificationService {
    void createNotification(Long userId, Long tenantId, String title, String message, NotificationType type, Long relatedTaskId);

    List<NotificationResponse> getUserNotifications(Long userId);

    List<NotificationResponse> getUnreadNotifications(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    long getUnreadCount(Long userId);
}

