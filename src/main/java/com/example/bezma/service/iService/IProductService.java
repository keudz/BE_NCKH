package com.example.bezma.service.iService;

import com.example.bezma.dto.req.product.CreateProductRequest;
import com.example.bezma.dto.res.product.ProductResponse;
import java.util.List;

public interface IProductService {
    List<ProductResponse> getProductsByTenant(Long tenantId);
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(Long id, CreateProductRequest request);
    void deleteProduct(Long id);
}
