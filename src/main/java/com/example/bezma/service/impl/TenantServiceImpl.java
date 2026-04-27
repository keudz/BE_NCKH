package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.tenant.TenantRegistrationRequest;
import com.example.bezma.dto.req.tenant.TenantUpdateRequest;
import com.example.bezma.dto.res.tenant.TenantDetailResponse;
//import com.example.bezma.dto.res.tenant.TenantSummaryResponse;
import com.example.bezma.entity.auth.Role;
//import com.example.bezma.entity.tenant.PlanType;
import com.example.bezma.entity.tenant.RegistrationStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.mapper.TenantMapper;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.ITenantService;
//import com.example.bezma.util.DataUtils;
import com.example.bezma.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
//import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements ITenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    private static final String REDIS_PREFIX = "t:s:"; // Nén key tiết kiệm 30MB
    private final UserRepository userRepository;
    // ==========================================
    // NHÓM 1: PUBLIC & SEO
    // ==========================================

    @Override
    public TenantDetailResponse getTenantBySlug(String slug) {
        String cacheKey = REDIS_PREFIX + slug;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null)
                return (TenantDetailResponse) cached;
        } catch (Exception e) {
            log.warn("Redis Error: {}", e.getMessage());
        }

        Tenant tenant = tenantRepository.findBySlugActive(slug)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        TenantDetailResponse response = tenantMapper.toDetailResponse(tenant);

        // Cache ngắn (5p) để xoay vòng bộ nhớ 30MB
        saveToCache(cacheKey, response, 5);
        return response;
    }

    @Override
    public boolean checkSlugAvailability(String slug) {
        return !tenantRepository.existsBySlugOrTenantCode(slug, null);
    }

    // ==========================================
    // NHÓM 2: REGISTRATION & ONBOARDING
    // ==========================================

    @Override
    @Transactional
    public TenantDetailResponse registerTenant(TenantRegistrationRequest request) {
        log.info("Bắt đầu đăng ký Tenant chuyên nghiệp: {}", request.getName());

        // 1. Kiểm tra trùng lặp: Tenant Code (nếu có) và Admin (Username/Email)
        if (userRepository.existsByUsername(request.getAdminPhone())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS); // Hoặc tạo ErrorCode mới nếu muốn
        }

        // 2. Tìm Role TENANT_ADMIN (Ưu tiên) hoặc ADMIN
        Role adminRole = roleRepository.findByName("TENANT_ADMIN")
                .or(() -> roleRepository.findByName("ADMIN"))
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // 3. Tạo Tenant từ Request (MapStruct sẽ map MST, Địa chỉ, Loại hình tự động)
        Tenant tenant = tenantMapper.toEntity(request);
        String token = UUID.randomUUID().toString();

        tenant.setRepresentativeName(request.getAdminFullName()); // Người đại diện
        tenant.setVerificationToken(token);
        tenant.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        tenant.setStatusConfirm(RegistrationStatus.PENDING_VERIFICATION);
        tenant.setActive(false); // Chưa confirm thì chưa active

        tenant = tenantRepository.save(tenant);

        // 4. Tạo Mật khẩu tạm thời (6 ký tự CHỮ HOA/SỐ + đuôi @)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String tempPassword = sb.toString() + "@";

        // 5. Tạo User Admin (Chủ sở hữu Tenant)
        User admin = User.builder()
                .username(request.getAdminPhone()) // Đăng nhập bằng SĐT cá nhân Admin
                .password(passwordEncoder.encode(tempPassword))
                .email(request.getAdminEmail()) // Email cá nhân Admin
                .fullName(request.getAdminFullName())
                .tenant(tenant)
                .role(adminRole)
                .isActive(false)
                .mustChangePassword(true) // BẮT BUỘC ĐỔI MK LẦN ĐẦU
                .status(UserStatus.PENDING_ACTIVE)
                .build();
        userRepository.save(admin);

        // 6. Lưu Password tạm vào Redis để gửi sau khi verify thành công
        String redisKey = "registration_checkpoint:" + tenant.getId();
        redisTemplate.opsForValue().set(redisKey, tempPassword, 24, TimeUnit.HOURS);

        // 7. Gửi mail xác thực
        emailService.sendVerificationEmail(tenant.getEmail(), token); // Gửi về Email doanh nghiệp

        return tenantMapper.toDetailResponse(tenant);
    }

    @Override
    @Transactional
    public boolean verifyTenant(String token) {
        log.info("Bắt đầu xác thực token: {}", token);

        // 1. Tìm Tenant theo token
        Tenant tenant = tenantRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        // 2. Check hết hạn token xác thực (cái này lưu ở DB)
        if (tenant.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        // 3. Lấy Password tạm từ Redis (Quả bom hẹn giờ)
        String redisKey = "registration_checkpoint:" + tenant.getId();
        Object cachedPassword = redisTemplate.opsForValue().get(redisKey);

        if (cachedPassword == null) {
            // Nếu Redis đã hết hạn thì coi như quá trình đăng ký thất bại (bị Listener xóa
            // rồi)
            throw new AppException(ErrorCode.REGISTRATION_TIMEOUT);
        }

        String tempPassword = cachedPassword.toString();

        // 4. Tìm User Admin của Tenant này để kích hoạt
        // Username lúc này chính là Phone đã đăng ký
        User admin = userRepository.findFirstByTenantId(tenant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 5. Cập nhật trạng thái "Xanh mượt" cho cả Tenant và User
        tenant.setIsVerified(true);
        tenant.setActive(true);
        tenant.setStatusConfirm(RegistrationStatus.VERIFIED);
        tenant.setVerificationToken(null);
        tenantRepository.save(tenant);

        admin.setIsActive(true);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        // 6. THÁO NGÒI NỔ: Xóa key trên Redis để Listener không nhảy vào xóa Tenant nữa
        redisTemplate.delete(redisKey);

        // 7. Gửi đúng tài khoản và mật khẩu tạm cho User
        emailService.sendCredentialsEmail(tenant.getEmail(), admin.getUsername(), tempPassword);

        log.info("Tenant {} và Admin {} đã được kích hoạt thành công!", tenant.getName(), admin.getUsername());
        return true;
    }

    // ==========================================
    // NHÓM 3: PLATFORM MANAGEMENT
    // ==========================================

    @Override
    @Transactional
    public TenantDetailResponse updateTenant(Long id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        tenantMapper.updateTenant(request, tenant);
        Tenant updated = tenantRepository.save(tenant);

        // Clear cache để khách nhận data mới
        redisTemplate.delete(REDIS_PREFIX + updated.getSlug());
        return tenantMapper.toDetailResponse(updated);
    }

    @Override
    public Page<TenantDetailResponse> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(tenantMapper::toDetailResponse);
    }

    @Override
    @Transactional
    public void changeTenantStatus(Long id, boolean active) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));
        tenant.setActive(active);
        tenantRepository.save(tenant);
        redisTemplate.delete(REDIS_PREFIX + tenant.getSlug());
    }

    // ==========================================
    // NHÓM 4: CRUD CƠ BẢN
    // ==========================================

    @Override
    public TenantDetailResponse getTenantById(Long id) {
        return tenantRepository.findById(id)
                .map(tenantMapper::toDetailResponse)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));
        tenant.setActive(false); // Soft delete
        tenantRepository.save(tenant);
        redisTemplate.delete(REDIS_PREFIX + tenant.getSlug());
    }

    @Override
    @Transactional
    public TenantDetailResponse createTenantManual(TenantRegistrationRequest request) {
        // Luồng Admin tạo trực tiếp, cho Verified luôn
        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setTenantCode("ADM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tenant.setIsVerified(true);
        tenant.setActive(true);
        tenant.setStatusConfirm(RegistrationStatus.VERIFIED);
        return tenantMapper.toDetailResponse(tenantRepository.save(tenant));
    }

    // Helper method để tái sử dụng và bắt lỗi Redis
    private void saveToCache(String key, Object data, int minutes) {
        try {
            redisTemplate.opsForValue().set(key, data, Duration.ofMinutes(minutes));
        } catch (Exception e) {
            log.error("Redis Full or Error, cannot cache key: {}", key);
        }
    }
}