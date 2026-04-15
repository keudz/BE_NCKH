package com.example.bezma.dto.req.task;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {
    private String title;
    private String description;
    private String priority;
    private String category;
    private LocalDateTime dueDate;
    private Long assigneeId;
    private Long tenantId;
}
