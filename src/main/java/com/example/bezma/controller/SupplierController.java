package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.supplier.SupplierRequest;
import com.example.bezma.dto.res.supplier.SupplierResponse;
import com.example.bezma.service.iService.ISupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "Quản lý nhà cung cấp")
public class SupplierController {

    private final ISupplierService supplierService;

    @Operation(summary = "Lấy danh sách nhà cung cấp của tenant hiện tại")
    @GetMapping
    public ApiResponse<List<SupplierResponse>> getAllSuppliers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ApiResponse.<List<SupplierResponse>>builder()
                .data(supplierService.getAllSuppliers(tenantId))
                .build();
    }

    @Operation(summary = "Tìm kiếm nhà cung cấp")
    @GetMapping("/search")
    public ApiResponse<List<SupplierResponse>> searchSuppliers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String keyword) {
        
        return ApiResponse.<List<SupplierResponse>>builder()
                .data(supplierService.searchSuppliers(tenantId, keyword))
                .build();
    }

    @Operation(summary = "Xem chi tiết nhà cung cấp")
    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> getSupplierById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ApiResponse.<SupplierResponse>builder()
                .data(supplierService.getSupplierById(id, tenantId))
                .build();
    }

    @Operation(summary = "Tạo nhà cung cấp mới")
    @PostMapping
    public ApiResponse<SupplierResponse> createSupplier(
            @Valid @RequestBody SupplierRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ApiResponse.<SupplierResponse>builder()
                .data(supplierService.createSupplier(tenantId, request))
                .message("Tạo nhà cung cấp thành công")
                .build();
    }

    @Operation(summary = "Cập nhật nhà cung cấp")
    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ApiResponse.<SupplierResponse>builder()
                .data(supplierService.updateSupplier(id, tenantId, request))
                .message("Cập nhật nhà cung cấp thành công")
                .build();
    }

    @Operation(summary = "Xóa nhà cung cấp")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSupplier(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        supplierService.deleteSupplier(id, tenantId);
        return ApiResponse.<Void>builder()
                .message("Xóa nhà cung cấp thành công")
                .build();
    }
}
