package com.example.bezma.service.impl;

import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.mapper.UserMapper;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IAdminService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserCreateResponse createUser(UserCreateRequest req, Long tenantId) {
        // 1. Tìm Tenant từ DB (Bắt buộc vì User cần object Tenant hoàn chỉnh)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant không tồn tại"));

        // 2. Map sang Entity (Lúc này tenant đã có data, MapStruct sẽ set vào User)
        User user = userMapper.toEntity(req, tenant);

        // 3. Set các thông tin bổ sung
        user.setPassword(passwordEncoder.encode("123456"));

        // Nếu req không có fullName, bạn có thể set mặc định
        if (user.getFullName() == null) {
            user.setFullName(req.getEmail());
        }

        // 4. Lưu và trả về
        return userMapper.toCreateResponse(userRepository.save(user));
    }
}
