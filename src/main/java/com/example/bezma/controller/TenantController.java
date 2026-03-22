package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import com.example.bezma.service.iService.ITenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final ITenantService tenantService;

    // ==========================================
    // NHÓM 1: PUBLIC APIs (Không cần Token)
    // ==========================================

    @PostMapping("/public/register")
    public ApiResponse<TenantDetailResponse> register(@RequestBody @Valid TenantRegistrationRequest request) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.registerTenant(request))
                .message("Đăng ký thành công! Vui lòng kiểm tra email xác thực.")
                .build();
    }

    @GetMapping("/public/verify")
    public ApiResponse<Boolean> verify(@RequestParam String token) {
        return ApiResponse.<Boolean>builder()
                .data(tenantService.verifyTenant(token))
                .message("Xác thực tài khoản thành công!")
                .build();
    }

    @GetMapping("/public/detail/{slug}")
    public ApiResponse<TenantDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.getTenantBySlug(slug))
                .build();
    }

    @GetMapping("/public/check-slug")
    public ApiResponse<Boolean> checkSlug(@RequestParam String slug) {
        return ApiResponse.<Boolean>builder()
                .data(tenantService.checkSlugAvailability(slug))
                .build();
    }

    // ==========================================
    // NHÓM 2: MANAGEMENT APIs (Cần quyền Admin/Owner)
    // ==========================================

    @GetMapping
    public ApiResponse<Page<TenantSummaryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        // Xử lý sort linh hoạt: field,direction
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sort[1]), sort[0]));

        return ApiResponse.<Page<TenantSummaryResponse>>builder()
                .data(tenantService.getAllTenants(pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.getTenantById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantDetailResponse> update(@PathVariable Long id, @RequestBody @Valid TenantUpdateRequest request) {
        return ApiResponse.<TenantDetailResponse>builder()
                .data(tenantService.updateTenant(id, request))
                .message("Cập nhật thông tin thành công!")
                .build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        tenantService.changeTenantStatus(id, active);
        return ApiResponse.<Void>builder()
                .message("Đã thay đổi trạng thái hoạt động!")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa đoàn lân khỏi hệ thống!")
                .build();
    }
}