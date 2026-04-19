package com.example.bezma.service.iService;

import com.example.bezma.dto.req.supplier.SupplierRequest;
import com.example.bezma.dto.res.supplier.SupplierResponse;

import java.util.List;

public interface ISupplierService {
    List<SupplierResponse> getAllSuppliers(Long tenantId);
    SupplierResponse getSupplierById(Long id, Long tenantId);
    SupplierResponse createSupplier(Long tenantId, SupplierRequest request);
    SupplierResponse updateSupplier(Long id, Long tenantId, SupplierRequest request);
    void deleteSupplier(Long id, Long tenantId);
    List<SupplierResponse> searchSuppliers(Long tenantId, String keyword);
}
