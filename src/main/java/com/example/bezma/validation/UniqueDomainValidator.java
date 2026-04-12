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
public class UniqueDomainValidator implements ConstraintValidator<UniqueDomain, String> {

    private final TenantRepository tenantRepository;

    @Override
    public boolean isValid(String domain, ConstraintValidatorContext context) {
        if (domain == null || domain.trim().isEmpty()) {
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
                    return !tenantRepository.existsByDomainAndIdNot(domain, tenantId);
                } catch (NumberFormatException e) {
                    // Ignore parsing error, fallback to normal check
                }
            }
        }

        return !tenantRepository.existsByDomain(domain);
    }
}
