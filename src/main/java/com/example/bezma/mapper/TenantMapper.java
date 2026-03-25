package com.example.bezma.mapper;

import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import com.example.bezma.entity.tenant.Tenant;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TenantMapper {
    @Mapping(target = "phone", source = "phone")
    Tenant toEntity(TenantRegistrationRequest request);
    TenantSummaryResponse toSummaryResponse(Tenant tenant);
    TenantDetailResponse toDetailResponse(Tenant tenant);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTenant(TenantUpdateRequest request, @MappingTarget Tenant tenant);
}
