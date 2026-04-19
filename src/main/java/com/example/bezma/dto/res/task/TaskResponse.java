package com.example.bezma.dto.res.task;

import lombok.Builder;
import lombok.Data;
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
}
