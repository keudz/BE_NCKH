package com.example.bezma.repository;

import com.example.bezma.entity.task.Task;
import com.example.bezma.entity.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByTenantId(Long tenantId);
    List<Task> findByTenantIdAndStatus(Long tenantId, TaskStatus status);
}
