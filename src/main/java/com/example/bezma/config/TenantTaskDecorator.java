package com.example.bezma.config;

import com.example.bezma.util.TenantContext;
import org.springframework.core.task.TaskDecorator;

public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long tenantId = TenantContext.getCurrentTenantId();
        return () -> {
            try {
                if (tenantId != null) {
                    TenantContext.setCurrentTenantId(tenantId);
                }
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
