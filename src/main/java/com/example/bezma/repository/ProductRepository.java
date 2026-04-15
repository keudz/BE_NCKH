package com.example.bezma.repository;

import com.example.bezma.entity.inventory.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTenantId(Long tenantId);
    List<Product> findByTenantIdAndCategory(Long tenantId, String category);
}
