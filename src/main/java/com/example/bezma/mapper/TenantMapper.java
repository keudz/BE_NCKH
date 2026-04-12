package com.example.bezma.mapper;

import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import com.example.bezma.entity.tenant.Tenant;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "isVerified", constant = "false")
    @Mapping(target = "statusConfirm", expression = "java(com.example.bezma.entity.tenant.RegistrationStatus.PENDING_VERIFICATION)")
    @Mapping(target = "users", ignore = true)
    Tenant toEntity(TenantRegistrationRequest request);

    @AfterMapping
    default void handleAutomaticFields(TenantRegistrationRequest request, @MappingTarget Tenant tenant) {
        if (tenant.getName() != null) {
            String slug = tenant.getName().toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");
            tenant.setSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 5));

            // Sinh mã định danh duy nhất cho Tenant
            tenant.setTenantCode("TN-" + System.currentTimeMillis());
        }
    }

    TenantSummaryResponse toSummaryResponse(Tenant tenant);

    @Mapping(target = "createdAt", source = "createdAt")
    TenantDetailResponse toDetailResponse(Tenant tenant);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantCode", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "verificationTokenExpiry", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "statusConfirm", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateTenant(TenantUpdateRequest request, @MappingTarget Tenant tenant);
}
