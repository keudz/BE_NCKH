package com.example.bezma.repository;

import com.example.bezma.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Page<Customer> findByTenantId(Long tenantId, Pageable pageable);
    List<Customer> findByTenantId(Long tenantId);

    java.util.Optional<Customer> findByTenantIdAndPhoneNumber(Long tenantId, String phoneNumber);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.phoneNumber LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);
    
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.phoneNumber LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Customer> searchCustomers(@Param("tenantId") Long tenantId, @Param("query") String query);
}
