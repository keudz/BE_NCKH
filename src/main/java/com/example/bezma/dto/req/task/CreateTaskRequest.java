package com.example.bezma.dto.req.task;

import lombok.Data;
import java.math.BigDecimal;
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
    private Long customerId;

    // Customer Info
    private String customerName;
    private String companyName;
    private String address;
    private String phoneNumber;
    private BigDecimal estimatedPrice;
}
