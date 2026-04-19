package com.example.bezma.service.impl;

import com.example.bezma.dto.req.supplier.SupplierRequest;
import com.example.bezma.dto.res.supplier.SupplierResponse;
import com.example.bezma.entity.supplier.Supplier;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.repository.SupplierRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.service.iService.ISupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements ISupplierService {

    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;

    @Override
    public List<SupplierResponse> getAllSuppliers(Long tenantId) {
        return supplierRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierResponse getSupplierById(Long id, Long tenantId) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp"));
        return mapToResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(Long tenantId, SupplierRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Tenant"));

        if (supplierRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new RuntimeException("Mã nhà cung cấp đã tồn tại");
        }

        Supplier supplier = Supplier.builder()
                .code(request.getCode())
                .name(request.getName())
                .logo(request.getLogo())
                .logoColor(request.getLogoColor())
                .contactName(request.getContactName())
                .contactTitle(request.getContactTitle())
                .phone(request.getPhone())
                .address(request.getAddress())
                .danhMuc(request.getDanhMuc())
                .trangThai(request.getTrangThai())
                .tenant(tenant)
                .build();

        supplier = supplierRepository.save(supplier);
        return mapToResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, Long tenantId, SupplierRequest request) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp"));

        if (!supplier.getCode().equals(request.getCode()) && 
            supplierRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new RuntimeException("Mã nhà cung cấp đã tồn tại");
        }

        supplier.setCode(request.getCode());
        supplier.setName(request.getName());
        supplier.setLogo(request.getLogo());
        supplier.setLogoColor(request.getLogoColor());
        supplier.setContactName(request.getContactName());
        supplier.setContactTitle(request.getContactTitle());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setDanhMuc(request.getDanhMuc());
        supplier.setTrangThai(request.getTrangThai());

        supplier = supplierRepository.save(supplier);
        return mapToResponse(supplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id, Long tenantId) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà cung cấp"));
        supplierRepository.delete(supplier);
    }

    @Override
    public List<SupplierResponse> searchSuppliers(Long tenantId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSuppliers(tenantId);
        }
        return supplierRepository.searchSuppliers(tenantId, keyword.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .logo(supplier.getLogo())
                .logoColor(supplier.getLogoColor())
                .contactName(supplier.getContactName())
                .contactTitle(supplier.getContactTitle())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .danhMuc(supplier.getDanhMuc())
                .trangThai(supplier.getTrangThai())
                .tenantId(supplier.getTenant().getId())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
