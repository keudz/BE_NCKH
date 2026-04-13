package com.example.bezma.service.iService;

import com.example.bezma.dto.req.admin.AssignRoleRequest;
import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;

public interface IAdminService {
    UserCreateResponse createUser(UserCreateRequest req, Long tenantId);
    void assignRole(Long tenantId, AssignRoleRequest request);
}
