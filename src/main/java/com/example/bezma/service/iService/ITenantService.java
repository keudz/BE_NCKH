package com.example.bezma.service.iService;

import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITenantService {

    // --- NHÓM 1: PUBLIC & SEO (Sử dụng Redis cực mạnh ở đây) ---

    // Lấy thông tin chi tiết cho Next.js render trang chủ đoàn lân
    TenantDetailResponse getTenantBySlug(String slug);

    // Check xem slug đã tồn tại chưa (Dùng khi khách gõ tên đoàn lân trên UI)
    boolean checkSlugAvailability(String slug);


    // --- NHÓM 2: REGISTRATION & ONBOARDING (Luồng xác thực) ---

    // Khách đăng ký Tenant mới
    TenantDetailResponse registerTenant(TenantRegistrationRequest request);

    // Xác thực Tenant qua Email Token
    boolean verifyTenant(String token);


    // --- NHÓM 3: PLATFORM MANAGEMENT (Dành cho Admin hệ thống) ---

    // Cập nhật thông tin đoàn lân (Chủ đoàn hoặc Admin làm)
    TenantDetailResponse updateTenant(Long id, TenantUpdateRequest request);

    // Lấy danh sách Tenant có phân trang (Tối ưu traffic so với lấy list thường)
    Page<TenantSummaryResponse> getAllTenants(Pageable pageable);

    // Khóa hoặc mở lại Tenant
    void changeTenantStatus(Long id, boolean active);

    // --- NHÓM 4: CRUD CƠ BẢN (Dành cho nội bộ/Admin) ---

    // Tìm theo ID (Cơ bản nhưng quan trọng để làm mấy luồng chi tiết)
    TenantDetailResponse getTenantById(Long id);

    // Xóa Tenant (Thường là Soft Delete - đánh dấu active=false chứ không xóa hẳn DB)
    void deleteTenant(Long id);

    // Nếu ông muốn có thêm Create trực tiếp từ Admin (không qua luồng verify)
    TenantDetailResponse createTenantManual(TenantRegistrationRequest request);
}