package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.finance.InvoiceRequest;
import com.example.bezma.dto.res.finance.InvoiceResponse;
import com.example.bezma.entity.finance.Invoice;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.InvoiceRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.service.iService.IInvoiceService;
import com.example.bezma.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(Long tenantId, InvoiceRequest request, MultipartFile file) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        // Tính toán thuế
        BigDecimal total = request.getTotalAmount();
        BigDecimal rate = request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = total.multiply(rate);
        BigDecimal finalAmount = total.add(taxAmount);

        String photoUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                photoUrl = cloudinaryService.uploadFile(file, "invoices");
            } catch (Exception e) {
                log.error("Lỗi lưu ảnh hóa đơn: {}", e.getMessage());
            }
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .totalAmount(total)
                .taxRate(rate)
                .taxAmount(taxAmount)
                .finalAmount(finalAmount)
                .invoiceDate(
                        request.getInvoiceDate() != null ? request.getInvoiceDate() : java.time.LocalDateTime.now())
                .description(request.getDescription())
                .type(request.getType())
                .photoUrl(photoUrl)
                .tenant(tenant)
                .build();

        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    public List<InvoiceResponse> getAllInvoices(Long tenantId) {
        return invoiceRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id, Long tenantId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MESSAGE));

        if (!invoice.getTenant().getId().equals(tenantId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        invoiceRepository.delete(invoice);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getTotalAmount())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .finalAmount(invoice.getFinalAmount())
                .invoiceDate(invoice.getInvoiceDate())
                .photoUrl(invoice.getPhotoUrl())
                .description(invoice.getDescription())
                .type(invoice.getType())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
