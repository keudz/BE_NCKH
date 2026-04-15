package com.example.bezma.dto.req.product;

import lombok.Data;

@Data
public class CreateProductRequest {
    private String name;
    private String sku;
    private String category;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private Long tenantId;
}
