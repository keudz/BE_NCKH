package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;
import com.example.bezma.service.iService.IAdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @Operation(summary = "Tạo người dùng mới cho Tenant")
    @PostMapping("tenants/{tenantId}/users")
    public ApiResponse<UserCreateResponse> createUser(@PathVariable("tenantId") Long tenantId,
            @RequestBody @Valid UserCreateRequest req) {

        return ApiResponse.<UserCreateResponse>builder()
                .data(adminService.createUser(req, tenantId))
                .message("Tạo người dùng thành công!")
                .build();
    }

}
