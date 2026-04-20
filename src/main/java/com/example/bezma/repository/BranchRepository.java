package com.example.bezma.repository;

import com.example.bezma.entity.branch.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    @Query("SELECT b FROM Branch b WHERE b.tenant.id = :tenantId AND b.active = true ORDER BY b.branchName ASC")
    List<Branch> findAllByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Branch b WHERE b.tenant.id = :tenantId AND b.active = true ORDER BY b.branchName ASC")
    Page<Branch> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT b FROM Branch b WHERE b.branchCode = :branchCode AND b.tenant.id = :tenantId AND b.active = true")
    Optional<Branch> findByBranchCodeAndTenant(@Param("branchCode") String branchCode, @Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Branch b WHERE b.id = :branchId AND b.tenant.id = :tenantId")
    Optional<Branch> findByIdAndTenant(@Param("branchId") Long branchId, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(b) > 0 FROM Branch b WHERE b.branchCode = :branchCode AND b.tenant.id = :tenantId AND b.id != :branchId")
    boolean existsByBranchCodeAndTenantExcluding(@Param("branchCode") String branchCode, @Param("tenantId") Long tenantId, @Param("branchId") Long branchId);

    @Query("SELECT COUNT(b) > 0 FROM Branch b WHERE b.branchCode = :branchCode AND b.tenant.id = :tenantId")
    boolean existsByBranchCodeAndTenant(@Param("branchCode") String branchCode, @Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Branch b WHERE b.manager.id = :managerId AND b.active = true")
    List<Branch> findByManager(@Param("managerId") Long managerId);

    @Query("SELECT b.id, b.branchCode, b.branchName, COUNT(DISTINCT u.id) as employeeCount FROM Branch b LEFT JOIN User u ON u.branch.id = b.id WHERE b.tenant.id = :tenantId AND b.active = true GROUP BY b.id")
    List<Object[]> findBranchSummaryByTenant(@Param("tenantId") Long tenantId);
}
