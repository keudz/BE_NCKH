package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.finance.InvoiceRequest;
import com.example.bezma.dto.res.finance.InvoiceResponse;
import com.example.bezma.service.iService.IInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice & Tax Management", description = "Quản lý hóa đơn và tính thuế")
public class InvoiceController {

    private final IInvoiceService invoiceService;

    @Operation(summary = "Lưu trữ hóa đơn mới và tính thuế")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<InvoiceResponse> createInvoice(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestPart("data") InvoiceRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        return ApiResponse.<InvoiceResponse>builder()
                .data(invoiceService.createInvoice(tenantId, request, file))
                .message("Lưu hóa đơn thành công")
                .build();
    }

    @Operation(summary = "Lấy danh sách hóa đơn")
    @GetMapping
    public ApiResponse<List<InvoiceResponse>> getAllInvoices(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return ApiResponse.<List<InvoiceResponse>>builder()
                .data(invoiceService.getAllInvoices(tenantId))
                .build();
    }

    @Operation(summary = "Xóa hóa đơn")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInvoice(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        invoiceService.deleteInvoice(id, tenantId);
        return ApiResponse.<Void>builder()
                .message("Xóa hóa đơn thành công")
                .build();
    }
}
