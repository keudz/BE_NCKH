package com.example.bezma.dto.res.project;

import com.example.bezma.entity.project.ProjectStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

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
    private List<ProjectMemberResponse> members;
}
