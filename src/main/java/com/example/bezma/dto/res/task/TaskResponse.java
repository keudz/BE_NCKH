package com.example.bezma.dto.res.task;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String date; // For display
    private LocalDateTime dueDate;
    private String avatar;
    private Long assigneeId;
    private String assigneeName;
    private String reportImages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Check-in fields ──
    private LocalDateTime checkInTime;
    private BigDecimal checkInLatitude;
    private BigDecimal checkInLongitude;
    private String checkInPhoto;
    private String checkInNote;

    // ── Completion fields ──
    private String completionPhoto;
    private LocalDateTime completionTime;
    private String resultNote;
    private Boolean customerConfirmed;

    // ── Computed ──
    private Long durationMinutes; // Thời gian thực hiện (phút) = completionTime - checkInTime
}
