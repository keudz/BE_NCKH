package com.example.bezma.repository;

import com.example.bezma.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTenantId(Long tenantId);
    List<Project> findByManagerId(Long managerId);
}
