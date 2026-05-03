package com.example.bezma.dto.res.finance;

import com.example.bezma.entity.finance.InvoiceType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String invoiceSymbol;
    private String taxCode;
    private String partnerName;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private LocalDateTime invoiceDate;
    private String photoUrl;
    private String description;
    private InvoiceType type;
    private LocalDateTime createdAt;
    private java.util.List<InvoiceItemResponse> items;
}
