package com.example.bezma.config;

import com.example.bezma.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    // Lắng nghe mọi phương thức được gọi bên trong các class thuộc package repository
    @Before("execution(* com.example.bezma.repository..*(..))")
    public void enableTenantFilter() {
        if (TenantContext.isTenantMode()) {
            Session session = entityManager.unwrap(Session.class);
            Long currentTenantId = TenantContext.getCurrentTenantId();

            // Bật filter và truyền ID vào
            session.enableFilter("tenantFilter").setParameter("tenantId", currentTenantId);
        } else {
            // Tắt filter nếu là Platform Admin (truy cập toàn quyền)
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter("tenantFilter");
        }
    }
}