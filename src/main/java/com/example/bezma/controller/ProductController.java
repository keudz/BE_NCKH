package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import com.example.bezma.service.iService.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Các API quản lý hàng hóa và tồn kho")
public class ProductController {

    private final IProductService productService;

    @Operation(summary = "Lấy danh sách sản phẩm theo Tenant")
    @GetMapping
    public ApiResponse<List<ProductResponse>> getByTenant(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return ApiResponse.<List<ProductResponse>>builder()
                .data(productService.getProductsByTenant(tenantId))
                .build();
    }

    @Operation(summary = "Thêm sản phẩm mới")
    @PostMapping
    public ApiResponse<ProductResponse> create(@RequestBody CreateProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .data(productService.createProduct(request))
                .message("Thêm sản phẩm thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật sản phẩm")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @RequestBody CreateProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .data(productService.updateProduct(id, request))
                .message("Cập nhật thành công!")
                .build();
    }

    @Operation(summary = "Xoá sản phẩm")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .message("Đã xoá sản phẩm khỏi kho!")
                .build();
    }
}
