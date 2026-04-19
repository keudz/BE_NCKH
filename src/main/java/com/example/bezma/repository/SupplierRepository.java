package com.example.bezma.repository;

import com.example.bezma.entity.supplier.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Page<Supplier> findByTenantId(Long tenantId, Pageable pageable);

    Optional<Supplier> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Supplier> searchSuppliers(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);
}
