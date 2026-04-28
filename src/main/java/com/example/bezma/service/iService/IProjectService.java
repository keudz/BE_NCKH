package com.example.bezma.service.iService;

import com.example.bezma.dto.req.project.CreateProjectRequest;
import com.example.bezma.dto.res.project.ProjectResponse;
import java.util.List;

public interface IProjectService {
    ProjectResponse createProject(CreateProjectRequest request, Long tenantId);
    List<ProjectResponse> getProjectsByTenant(Long tenantId);
    List<ProjectResponse> getProjectsByManager(Long managerId);
    ProjectResponse getProjectDetail(Long projectId);
    void addMemberToProject(Long projectId, Long userId);
    void removeMemberFromProject(Long projectId, Long userId);
}
