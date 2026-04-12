package com.example.bezma.mapper;

import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.req.user.UserRegistrationRequest;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;
import com.example.bezma.dto.res.user.UserDetailResponse;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING_ACTIVE")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "verificationTokenExpiry", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(UserRegistrationRequest request);

    UserCreateResponse toCreateResponse(User user);

    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "tenantCode", source = "tenant.tenantCode")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "slug", ignore = true)
    UserSummaryResponse toSummaryResponse(User user);

    @Mapping(target = "avatarZalo", source = "avatar")
    @Mapping(target = "subPhone", source = "phone")
    UserDetailResponse toDetailResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);
}