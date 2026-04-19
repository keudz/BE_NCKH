package com.example.bezma.dto.res.notification;

import com.example.bezma.entity.notification.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Long relatedTaskId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

