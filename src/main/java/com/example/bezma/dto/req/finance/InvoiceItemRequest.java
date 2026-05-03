package com.example.bezma.dto.req.finance;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
