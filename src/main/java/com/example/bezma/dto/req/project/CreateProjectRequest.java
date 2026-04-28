package com.example.bezma.dto.req.project;

import com.example.bezma.entity.project.ProjectStatus;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateProjectRequest {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long managerId;
    private ProjectStatus status;
}
