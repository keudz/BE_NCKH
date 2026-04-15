package com.example.bezma.service.impl;

import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import com.example.bezma.entity.inventory.Product;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.repository.ProductRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.service.iService.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;

    @Override
    public List<ProductResponse> getProductsByTenant(Long tenantId) {
        return productRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .category(request.getCategory())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .tenant(tenant)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        String status = "CÒN HÀNG";
        if (product.getStock() == 0) status = "HẾT HÀNG";
        else if (product.getStock() <= 5) status = "SẮP HẾT";

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .category(product.getCategory())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(status)
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
