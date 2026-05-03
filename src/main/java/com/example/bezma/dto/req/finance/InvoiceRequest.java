package com.example.bezma.dto.req.finance;

import com.example.bezma.entity.finance.InvoiceType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceRequest {
    private String invoiceNumber;
    private String invoiceSymbol;
    private String taxCode;
    private String partnerName;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal taxRate;
    private LocalDateTime invoiceDate;
    private String description;
    private InvoiceType type;
    private java.util.List<InvoiceItemRequest> items;
}
