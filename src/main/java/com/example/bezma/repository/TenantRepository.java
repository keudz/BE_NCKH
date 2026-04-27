package com.example.bezma.repository;

import com.example.bezma.entity.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    @Query("SELECT t FROM Tenant t LEFT JOIN FETCH t.users WHERE t.slug = :slug AND t.active = true")
    Optional<Tenant> findBySlugActive(@Param("slug") String slug);
    @Query("SELECT COUNT(t) > 0 FROM Tenant t WHERE t.slug = :slug OR t.tenantCode = :code")
    boolean existsBySlugOrTenantCode(@Param("slug") String slug, @Param("code") String code);
    @Query("SELECT t FROM Tenant t WHERE t.tenantCode = :code AND t.active = true")
    Optional<Tenant> findByTenantCode(@Param("code") String code);
    @Query("SELECT t.id as id, t.name as name, t.logo as logo FROM Tenant t WHERE t.id = :id")
    Object findSummaryById(@Param("id") Long id);
    Optional<Tenant> findByVerificationToken(String token);
    Optional<Tenant> findById(Long id);
    boolean existsByDomain(String domain);
    boolean existsByDomainAndIdNot(String domain, Long id);
    boolean existsByEmail(String email);
    boolean existsByTenantCode(String tenantCode);
    boolean existsByTenantCodeAndIdNot(String tenantCode, Long id);
}