package com.example.bezma.service.impl;

import com.example.bezma.dto.req.project.CreateProjectRequest;
import com.example.bezma.dto.res.project.ProjectResponse;
import com.example.bezma.entity.project.Project;
import com.example.bezma.entity.project.ProjectMember;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.ProjectMemberRepository;
import com.example.bezma.repository.ProjectRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .tenant(tenant)
                .manager(manager)
                .build();

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Override
    public List<ProjectResponse> getProjectsByTenant(Long tenantId) {
        return projectRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getProjectsByManager(Long managerId) {
        return projectRepository.findByManagerId(managerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponse getProjectDetail(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
        return mapToResponse(project);
    }

    @Override
    @Transactional
    public void addMemberToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            ProjectMember member = ProjectMember.builder()
                    .project(project)
                    .user(user)
                    .build();
            projectMemberRepository.save(member);
        }
    }

    @Override
    @Transactional
    public void removeMemberFromProject(Long projectId, Long userId) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .forEach(projectMemberRepository::delete);
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus())
                .managerId(project.getManager() != null ? project.getManager().getId() : null)
                .managerName(project.getManager() != null ? project.getManager().getFullName() : null)
                .tenantId(project.getTenant().getId())
                .build();
    }
}
