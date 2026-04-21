package com.example.bezma.controller;

import com.example.bezma.dto.res.notification.NotificationResponse;
import com.example.bezma.service.iService.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import com.example.bezma.common.res.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Các API quản lý thông báo real-time")
public class NotificationController {

    private final INotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "Lấy danh sách thông báo")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.<List<NotificationResponse>>builder()
                .data(notificationService.getUserNotifications(userId))
                .build();
    }

    @Operation(summary = "Lấy danh sách thông báo chưa đọc")
    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.<List<NotificationResponse>>builder()
                .data(notificationService.getUnreadNotifications(userId))
                .build();
    }

    @Operation(summary = "Đếm số thông báo chưa đọc")
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.<Long>builder()
                .data(notificationService.getUnreadCount(userId))
                .build();
    }

    @Operation(summary = "Đánh dấu thông báo đã đọc")
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ApiResponse.<Void>builder()
                .message("Thông báo đã được đánh dấu đã đọc!")
                .build();
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead(
            @RequestAttribute("userId") Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.<Void>builder()
                .message("Tất cả thông báo đã được đánh dấu đã đọc!")
                .build();
    }

    /**
     * Gửi notification qua WebSocket
     * được gọi từ service khi có sự kiện
     */
    public void sendNotificationToUser(Long userId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }
}
