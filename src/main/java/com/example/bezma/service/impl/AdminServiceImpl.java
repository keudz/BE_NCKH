package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.admin.AssignRoleRequest;
import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.mapper.UserMapper;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IAdminService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper; // Vẫn giữ để map Response cuối cùng nếu cần

    @Override
    @Transactional
    public UserCreateResponse createUser(UserCreateRequest req, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant không tồn tại"));

        // Gán Role mặc định là STAFF (Nhân viên) cho user mới tạo bởi Admin
        Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new RuntimeException("Role STAFF không tồn tại"));

        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getEmail());
        user.setPassword(passwordEncoder.encode("123456")); // password nullable = false
        user.setTenant(tenant);
        user.setRole(staffRole);
        user.setStatus(UserStatus.PENDING_ACTIVE);
        user.setIsActive(true);
        user.setCode("USER_" + System.currentTimeMillis());

        User savedUser = userRepository.save(user);

        // Cách cũ: Map thủ công
        /*
         * UserCreateResponse response = new UserCreateResponse();
         * response.setId(savedUser.getId());
         * response.setEmail(savedUser.getEmail());
         * return response;
         */
        return userMapper.toCreateResponse(savedUser);
    }

    @Override
    @Transactional
    public void assignRole(Long tenantId, AssignRoleRequest req) {

        User user = userRepository.findByIdAndTenantId(req.getUserId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getRole() != null && req.getRole().equals(user.getRole().getName())) {
            throw new AppException(ErrorCode.USER_ALREADY_EMPLOYEE);
        }

        Role role = roleRepository.findByName(req.getRole())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(role);

        userRepository.save(user);
    }
}
