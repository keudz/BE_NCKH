package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
//import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import com.example.bezma.service.iService.ITenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Các API liên quan đến thao tác doanh nghiệp")
public class TenantController {

    private final ITenantService tenantService;

    // ==========================================
    // NHÓM 1: PUBLIC APIs (Không cần Token)
    // ==========================================

    @Operation(summary = "Đăng ký Tenant mới")
    @PostMapping("/public/register")
    public ApiResponse<TenantDetailResponse> register(@RequestBody @Valid TenantRegistrationRequest request) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.registerTenant(request))
                .message("Đăng ký thành công! Vui lòng kiểm tra email xác thực.")
                .build();
    }

    @Operation(summary = "Xác thực Tenant qua token")
    @GetMapping("/public/verify")
    public ApiResponse<Boolean> verify(@RequestParam String token) {
        return ApiResponse.<Boolean>builder()
                .data(tenantService.verifyTenant(token))
                .message("Xác thực tài khoản thành công!")
                .build();
    }

    @Operation(summary = "Lấy thông tin public của Tenant qua Slug")
    @GetMapping("/public/detail/{slug}")
    public ApiResponse<TenantDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.getTenantBySlug(slug))
                .build();
    }

    @Operation(summary = "Kiểm tra Slug có khả dụng không")
    @GetMapping("/public/check-slug")
    public ApiResponse<Boolean> checkSlug(@RequestParam String slug) {
        return ApiResponse.<Boolean>builder()
                .data(tenantService.checkSlugAvailability(slug))
                .build();
    }

    // ==========================================
    // NHÓM 2: MANAGEMENT APIs (Cần quyền Admin/Owner)
    // ==========================================

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả Tenant (Admin)")
    @GetMapping
    public ApiResponse<Page<TenantDetailResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        // Xử lý sort linh hoạt: field,direction
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sort[1]), sort[0]));

        return ApiResponse.<Page<TenantDetailResponse>>builder()
                .data(tenantService.getAllTenants(pageable))
                .build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lấy thông tin chi tiết Tenant (Admin)")
    @GetMapping("/{id}")
    public ApiResponse<TenantDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.getTenantById(id))
                .build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cập nhật thông tin Tenant (Admin)")
    @PutMapping("/{id}")
    public ApiResponse<TenantDetailResponse> update(@PathVariable Long id,
            @RequestBody @Valid TenantUpdateRequest request) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.updateTenant(id, request))
                .message("Cập nhật thông tin thành công!")
                .build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Thay đổi trạng thái hoạt động của Tenant (Admin)")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        tenantService.changeTenantStatus(id, active);
        return ApiResponse.<Void>builder()
                .message("Đã thay đổi trạng thái hoạt động!")
                .build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Xóa Tenant (Soft delete) (Admin)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa đoàn lân khỏi hệ thống!")
                .build();
    }
}