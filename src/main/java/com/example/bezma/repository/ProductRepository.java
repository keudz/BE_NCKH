package com.example.bezma.repository;

import com.example.bezma.entity.inventory.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByTenantId(Long tenantId, Pageable pageable);
    List<Product> findByTenantId(Long tenantId);
    
    Page<Product> findByTenantIdAndCategory(Long tenantId, String category, Pageable pageable);
    List<Product> findByTenantIdAndCategory(Long tenantId, String category);

    long countByTenantId(Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.price * p.stock) FROM Product p WHERE p.tenant.id = :tenantId")
    Double calculateInventoryValue(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.id = :id")
    java.util.Optional<Product> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
}
