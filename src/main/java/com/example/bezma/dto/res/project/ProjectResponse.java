package com.example.bezma.dto.res.project;

import com.example.bezma.entity.project.ProjectStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private Long managerId;
    private String managerName;
    private Long tenantId;
}
