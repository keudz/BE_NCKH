package com.example.bezma.service.impl;

import com.example.bezma.dto.res.notification.NotificationResponse;
import com.example.bezma.entity.notification.Notification;
import com.example.bezma.entity.notification.NotificationType;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.NotificationRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(Long userId, Long tenantId, String title, String message, NotificationType type, Long relatedTaskId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Notification notification = Notification.builder()
                .user(user)
                .tenant(tenant)
                .title(title)
                .message(message)
                .type(type)
                .relatedTaskId(relatedTaskId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        // NotificationPublisher sẽ gửi WebSocket sau khi nhận response này
        // Không gửi ở đây để tránh duplicate
        return mapToResponse(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadNotificationsByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadNotificationsByUserId(userId);
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .relatedTaskId(notification.getRelatedTaskId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}

