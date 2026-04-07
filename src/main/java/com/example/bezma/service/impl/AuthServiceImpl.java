package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.security.JwtTokenProvider;
import com.example.bezma.service.iService.IAuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ZaloServiceImpl zaloService;
    private RoleRepository roleRepository;
    private TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE); // Dùng ErrorCode chuẩn
            }

            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .username(user.getUsername())
                    .role(user.getRole().getName())
                    .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                    .build();

        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.VALIDATE_LOGIN);
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // 1. Kiểm tra Refresh Token có hợp lệ và chưa hết hạn không
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
        }

        // 2. Lấy username từ token
        String username = jwtTokenProvider.getUsernameFromToken(requestRefreshToken);


        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa!");
        }

        // 4. Tạo bộ Token mới toanh
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getUsername())
                .role(user.getRole().getName())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .build();
    }


    @Override
    @Transactional
    public AuthResponse loginZalo(ZaloLoginRequest request) {

        String zaloId;
        try {
            zaloId = zaloService.getZaloIdFromToken(request.getZaloToken());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByZaloId(zaloId).orElse(null);

        if (user == null) {

            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

            Role customerRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            user = User.builder()
                    .zaloId(zaloId)
                    .username(zaloId)
                    .password(passwordEncoder.encode("ZALO_SSO_RANDOM_" + System.currentTimeMillis()))
                    .fullName("Khách hàng Zalo")
                    .tenant(tenant)
                    .role(customerRole)
                    .isActive(true)
                    .isVerified(true)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(user);
        } else {

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE);
            }

            if (!user.getTenant().getId().equals(request.getTenantId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().getName())
                .tenantId(user.getTenant().getId())
                .build();
    }

}