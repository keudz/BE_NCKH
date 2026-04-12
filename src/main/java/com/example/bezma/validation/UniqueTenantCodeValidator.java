package com.example.bezma.validation;

import com.example.bezma.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@RequiredArgsConstructor
public class UniqueTenantCodeValidator implements ConstraintValidator<UniqueTenantCode, String> {

    private final TenantRepository tenantRepository;

    @Override
    public boolean isValid(String tenantCode, ConstraintValidatorContext context) {
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            return true;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            @SuppressWarnings("unchecked")
            Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

            if (pathVariables != null && pathVariables.containsKey("id")) {
                try {
                    Long tenantId = Long.valueOf(pathVariables.get("id"));
                    return !tenantRepository.existsByTenantCodeAndIdNot(tenantCode, tenantId);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        return !tenantRepository.existsByTenantCode(tenantCode);
    }
}
