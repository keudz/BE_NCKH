package com.example.bezma.dto.req.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    private String title;
    private String description;
    private String priority;
    private String category;
    private LocalDateTime dueDate;
    private Long assigneeId;
    private Long tenantId;
    private Long customerId;
    private Long projectId;
    private Boolean requirePhoto;

    // Customer Info
    private String customerName;
    private String companyName;
    private String address;
    private String phoneNumber;
    private BigDecimal estimatedPrice;
}
