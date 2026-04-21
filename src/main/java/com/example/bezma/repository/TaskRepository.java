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

    // --- Dùng cho Admin ---
    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, TaskStatus status);

    long countByTenantIdAndStatusAndCreatedAtBetween(Long tenantId, TaskStatus status, java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    // --- Dùng cho Nhân viên ---
    long countByAssigneeId(Long assigneeId);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    // --- Dùng cho Task Workflow ---
    List<Task> findByTenantIdAndAssigneeIsNull(Long tenantId);

    List<Task> findTop5ByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @org.springframework.data.jpa.repository.Query(value = 
        "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as day, COUNT(*) as count " +
        "FROM system_tasks WHERE tenant_id = :tenantId AND status = 'DONE' " +
        "AND created_at >= :startDate " +
        "GROUP BY day ORDER BY day ASC", nativeQuery = true)
    List<Object[]> countCompletedTasksGroupByDay(@org.springframework.data.repository.query.Param("tenantId") Long tenantId, 
                                                 @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);
}
