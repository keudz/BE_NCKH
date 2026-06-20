package com.example.bezma.config;

import com.example.bezma.util.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    @Autowired
    private EntityManager entityManager;

    @Before("execution(* com.example.bezma.repository..*(..))")
    public void enableTenantFilter() {
        if (TenantContext.isTenantMode()) {
            try {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.getCurrentTenantId());
            } catch (Exception e) {
                // Tránh làm crash ứng dụng nếu session không hoạt động hoặc không unwrap được
            }
        }
    }
}
