package com.example.bezma.service.impl;

import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest;
import com.example.bezma.dto.req.auth.ZaloLoginRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.security.JwtTokenProvider;
import com.example.bezma.service.iService.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ZaloServiceImpl zaloService;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt hoặc đang bị khoá!");
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

    // Nhớ import ZaloService và thêm vào phần @RequiredArgsConstructor


    @Override
    public AuthResponse loginZalo(ZaloLoginRequest request) {
        // Bước 4 (trong ảnh): Xác thực chéo lấy Zalo ID
        String zaloId = zaloService.getZaloIdFromToken(request.getZaloToken());

        // Bước 5 (trong ảnh): Tìm User và check B2B Tenant
        User user = userRepository.findByZaloId(zaloId)
                .orElseThrow(() -> new RuntimeException("Tài khoản Zalo này chưa được liên kết với hệ thống!"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa!");
        }

        // BẢO MẬT MULTI-TENANT: Nick Zalo này có đúng là nhân viên của công ty (tenantId) này không?
        if (!user.getTenant().getId().equals(request.getTenantId())) {
            throw new RuntimeException("Tài khoản Zalo của bạn không thuộc doanh nghiệp này!");
        }

        // Bước 6 (trong ảnh): Cấp chìa khóa (Tạo Token của riêng hệ thống BE)
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