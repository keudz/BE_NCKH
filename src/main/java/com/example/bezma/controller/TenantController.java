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
//import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
//import java.net.URI;

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
    @GetMapping(value = "/public/verify", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String verify(@RequestParam String token) {
        tenantService.verifyTenant(token);
        return """
                <html>
                    <head>
                        <title>Verification Success</title>
                        <style>
                            body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f8fafc; font-family: 'Arial', sans-serif; }
                            .card { background: white; padding: 50px 80px; border-radius: 24px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1); text-align: center; border: 1px solid #e2e8f0; }
                            h1 { color: #16a34a; font-size: 64px; margin: 0; font-weight: 900; letter-spacing: -0.025em; }
                            p { color: #475569; margin-top: 16px; font-size: 20px; font-weight: 500; }
                            .icon { font-size: 48px; margin-bottom: 20px; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <div class="icon">✅</div>
                            <h1>SUCCESS!</h1>
                            <p>Xác thực tài khoản thành công
                                <br>Hãy Chờ Mail Thông Tin Tài Khoản Đăng Nhập được gửi về</p>
                        </div>
                    </body>
                </html>
                """;
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