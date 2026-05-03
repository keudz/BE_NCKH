package com.example.bezma.service.impl;

import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import com.example.bezma.entity.inventory.Product;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.repository.ProductRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.service.iService.IProductService;
import com.example.bezma.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.bezma.common.res.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public PageResponse<ProductResponse> getProductsByTenant(Long tenantId, String category, String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;
        
        if (category != null && !category.isEmpty() && !"Tất cả".equals(category)) {
            productPage = productRepository.findByTenantIdAndCategory(tenantId, category, pageable);
        } else {
            productPage = productRepository.findByTenantId(tenantId, pageable);
        }

        // Apply search filter in memory for now if there is search or status, otherwise just return mapped page
        List<ProductResponse> filteredList = productPage.getContent().stream()
                .map(this::mapToResponse)
                .filter(p -> search == null || search.isEmpty() || 
                        (p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase())) || 
                        (p.getSku() != null && p.getSku().toLowerCase().contains(search.toLowerCase())))
                .filter(p -> status == null || status.isEmpty() || "Tất cả".equals(status) || p.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(filteredList)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Override
    public List<ProductResponse> getProductsByTenant(Long tenantId, String category, String search, String status) {
        return productRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .filter(p -> category == null || category.isEmpty() || "Tất cả".equals(category) || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> search == null || search.isEmpty() || 
                        (p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase())) || 
                        (p.getSku() != null && p.getSku().toLowerCase().contains(search.toLowerCase())))
                .filter(p -> status == null || status.isEmpty() || "Tất cả".equals(status) || p.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, MultipartFile image) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        String imageUrl = request.getImageUrl();
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadFile(image, "products");
            } catch (Exception e) {
                log.error("Lỗi upload ảnh sản phẩm: {}", e.getMessage());
            }
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .category(request.getCategory())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(imageUrl)
                .tenant(tenant)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, Long tenantId, CreateProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized: Bạn không có quyền cập nhật sản phẩm này");
        }

        String imageUrl = request.getImageUrl();
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadFile(image, "products");
            } catch (Exception e) {
                log.error("Lỗi upload ảnh sản phẩm: {}", e.getMessage());
            }
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(imageUrl);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long tenantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized: Bạn không có quyền xoá sản phẩm này");
        }
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
