//package com.example.bezma.service.impl;
//
//import com.example.bezma.mapper.AuthMapper;
//import com.example.bezma.repository.UserRepository;
//import com.example.bezma.security.JwtTokenProvider;
//import com.example.bezma.service.iService.IAuthService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AuthServiceImpl implements IAuthService {
//    private final AuthenticationManager authenticationManager;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final UserRepository userRepository;
//    private final AuthMapper authMapper;
//}
package com.example.bezma.service.impl;

import com.example.bezma.mapper.AuthMapper;
import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RegisterRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.security.JwtTokenProvider;
import com.example.bezma.service.iService.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = (User) authentication.getPrincipal();

            AuthResponse response = authMapper.toAuthResponse(
                    jwtTokenProvider.generateAccessToken(user),
                    jwtTokenProvider.generateRefreshToken(user));
            response.setUser(authMapper.toUserDetail(user));

            return response;
        } catch (Exception e) {
            log.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Get default role and tenant
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // For simplicity, we use the first tenant if one exists, otherwise create a
        // default one
        Tenant tenant = tenantRepository.findByTenantCode("DEFAULT")
                .orElseGet(() -> {
                    Tenant defaultTenant = new Tenant();
                    defaultTenant.setName("Default Tenant");
                    defaultTenant.setTenantCode("DEFAULT");
                    defaultTenant.setSlug("default-tenant");
                    defaultTenant.setActive(true);
                    defaultTenant.setIsVerified(true);
                    return tenantRepository.save(defaultTenant);
                });

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .zaloId(request.getZaloId())
                .role(userRole)
                .tenant(tenant)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .build();

        userRepository.save(user);

        AuthResponse response = authMapper.toAuthResponse(
                jwtTokenProvider.generateAccessToken(user),
                jwtTokenProvider.generateRefreshToken(user));
        response.setUser(authMapper.toUserDetail(user));

        return response;
    }

    @Override
    @Transactional
    public AuthResponse loginWithZalo(ZaloLoginRequest request) {
        log.info("Zalo login attempt for zaloId: {}, phone: {}", request.getZaloId(), request.getPhone());

        // 1. Tìm theo zaloId trước (người dùng đã map thành công trước đó)
        Optional<User> userOptional = userRepository.findByZaloId(request.getZaloId());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // 2. Nếu chưa có zaloId, tìm theo số điện thoại (để map tài khoản Admin đã tạo)
            log.info("zaloId {} not found, trying to map by phone: {}", request.getZaloId(), request.getPhone());
            
            user = userRepository.findByPhone(request.getPhone())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // 3. Nếu tìm thấy theo phone, thực hiện map zaloId vào tài khoản này
            if (user.getZaloId() == null) {
                log.info("Mapping zaloId {} to existing user with phone {}", request.getZaloId(), request.getPhone());
                user.setZaloId(request.getZaloId());
                
                // Cập nhật thêm avatar/fullName từ Zalo nếu Admin chưa set hoặc muốn đồng bộ
                if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
                    user.setAvatar(request.getAvatar());
                }
                
                userRepository.save(user);
            } else {
                // Trường hợp hy hữu: phone này đã được map với một zaloId KHÁC
                log.warn("Phone {} is already linked to another zaloId", request.getPhone());
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS); 
            }
        }

        // 4. Kiểm tra trạng thái tài khoản
        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        // 5. Tạo JWT Token và trả về thông tin User
        AuthResponse response = authMapper.toAuthResponse(
                jwtTokenProvider.generateAccessToken(user),
                jwtTokenProvider.generateRefreshToken(user));
        response.setUser(authMapper.toUserDetail(user));

        return response;
    }
}
