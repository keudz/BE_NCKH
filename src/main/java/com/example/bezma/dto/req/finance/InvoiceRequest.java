package com.example.bezma.dto.req.finance;

import com.example.bezma.entity.finance.InvoiceType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceRequest {
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal taxRate;
    private LocalDateTime invoiceDate;
    private String description;
    private InvoiceType type;
}
