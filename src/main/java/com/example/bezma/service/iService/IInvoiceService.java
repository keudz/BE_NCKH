package com.example.bezma.service.iService;

import com.example.bezma.dto.req.finance.InvoiceRequest;
import com.example.bezma.dto.res.finance.InvoiceResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface IInvoiceService {
    InvoiceResponse createInvoice(Long tenantId, InvoiceRequest request, MultipartFile file);
    List<InvoiceResponse> getAllInvoices(Long tenantId);
    void deleteInvoice(Long id, Long tenantId);
}
