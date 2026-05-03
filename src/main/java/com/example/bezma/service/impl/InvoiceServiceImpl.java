package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.finance.InvoiceRequest;
import com.example.bezma.dto.res.finance.InvoiceResponse;
import com.example.bezma.dto.res.finance.InvoiceItemResponse;
import com.example.bezma.entity.finance.Invoice;
import com.example.bezma.entity.finance.PaymentMethod;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.InvoiceRepository;
import com.example.bezma.repository.ProductRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.entity.inventory.Product;
import com.example.bezma.entity.finance.InvoiceItem;
import com.example.bezma.entity.finance.InvoiceType;
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
    private final ProductRepository productRepository;
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
                .invoiceSymbol(request.getInvoiceSymbol())
                .taxCode(request.getTaxCode())
                .partnerName(request.getPartnerName())
                .paymentMethod(request.getPaymentMethod() != null ? PaymentMethod.valueOf(request.getPaymentMethod()) : PaymentMethod.CK)
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

        // Xử lý Invoice Items và Cập nhật Kho
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            java.util.List<InvoiceItem> items = new java.util.ArrayList<>();
            for (var itemReq : request.getItems()) {
                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .productName(itemReq.getProductName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .totalAmount(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                        .build();

                    // LOGIC CẬP NHẬT KHO THÔNG MINH
                    Product product;
                    if (itemReq.getProductId() != null) {
                        product = productRepository.findByIdWithLock(itemReq.getProductId())
                                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                    } else if (request.getType() == InvoiceType.BUY) {
                        // TỰ ĐỘNG TẠO SẢN PHẨM MỚI NẾU ĐANG NHẬP HÀNG
                        product = Product.builder()
                                .name(itemReq.getProductName())
                                .price(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(1.2)).doubleValue()) // Ép kiểu về Double
                                .stock(0)
                                .sku("AUTO-" + System.currentTimeMillis() % 100000)
                                .tenant(tenant)
                                .build();
                        log.info("Tự động tạo sản phẩm mới từ hóa đơn nhập: {}", itemReq.getProductName());
                    } else {
                        // Nếu là Hóa đơn BÁN mà không có ID sản phẩm thì bỏ qua (vì không biết trừ vào đâu)
                        items.add(item);
                        continue;
                    }

                    item.setProduct(product);
                    item.setProductName(product.getName());
                    
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    if (request.getType() == InvoiceType.SELL) {
                        if (currentStock < itemReq.getQuantity()) {
                            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
                        }
                        product.setStock(currentStock - itemReq.getQuantity());
                    } else if (request.getType() == InvoiceType.BUY) {
                        product.setStock(currentStock + itemReq.getQuantity());
                    }
                    productRepository.save(product);
                items.add(item);
            }
            invoice.setItems(items);
        }

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
                .invoiceSymbol(invoice.getInvoiceSymbol())
                .taxCode(invoice.getTaxCode())
                .partnerName(invoice.getPartnerName())
                .paymentMethod(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : null)
                .totalAmount(invoice.getTotalAmount())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .finalAmount(invoice.getFinalAmount())
                .invoiceDate(invoice.getInvoiceDate())
                .photoUrl(invoice.getPhotoUrl())
                .description(invoice.getDescription())
                .type(invoice.getType())
                .createdAt(invoice.getCreatedAt())
                .items(invoice.getItems() != null ? invoice.getItems().stream().map((InvoiceItem item) -> 
                    InvoiceItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalAmount(item.getTotalAmount())
                        .build()
                ).collect(Collectors.toList()) : null)
                .build();
    }
}
