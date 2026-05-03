package com.example.bezma.service.iService;

import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import org.springframework.web.multipart.MultipartFile;
import com.example.bezma.common.res.PageResponse;
import java.util.List;

public interface IProductService {
    PageResponse<ProductResponse> getProductsByTenant(Long tenantId, String category, String search, String status, int page, int size);
    List<ProductResponse> getProductsByTenant(Long tenantId, String category, String search, String status);
    ProductResponse createProduct(CreateProductRequest request, MultipartFile image);
    ProductResponse updateProduct(Long id, Long tenantId, CreateProductRequest request, MultipartFile image);
    void deleteProduct(Long id, Long tenantId);
}
