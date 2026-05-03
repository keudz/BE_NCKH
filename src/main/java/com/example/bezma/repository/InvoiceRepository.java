package com.example.bezma.repository;

import com.example.bezma.entity.finance.Invoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    @EntityGraph(attributePaths = {"items"})
    List<Invoice> findByTenantId(Long tenantId);
}
