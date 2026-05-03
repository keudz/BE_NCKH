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

import com.example.bezma.dto.res.user.UserSummaryResponse;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ZaloServiceImpl zaloService;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final com.example.bezma.util.EmailService emailService;

    private UserSummaryResponse mapToUserSummaryResponse(User user) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .tenantCode(user.getTenant() != null ? user.getTenant().getTenantCode() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .mustChangePassword(user.getMustChangePassword())
                .birthday(user.getBirthday() != null ? user.getBirthday().format(formatter) : null)
                .gender(user.getGender())
                .address(user.getAddress())
                .identityCard(user.getIdentityCard())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = (User) authentication.getPrincipal();

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE);
            }

            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(mapToUserSummaryResponse(user))
                    .build();

        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.VALIDATE_LOGIN);
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
        }

        String username = jwtTokenProvider.getUsernameFromToken(requestRefreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa!");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserSummaryResponse(user))
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

            Role staffRole = roleRepository.findByName("STAFF")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            user = User.builder()
                    .zaloId(zaloId)
                    .username(zaloId)
                    .password(passwordEncoder.encode("ZALO_SSO_RANDOM_" + System.currentTimeMillis()))
                    .fullName(request.getFullName() != null ? request.getFullName() : "Nhân viên Zalo")
                    .avatar(request.getAvatar())
                    .tenant(tenant)
                    .role(staffRole)
                    .isActive(true)
                    .isVerified(true)
                    .status(UserStatus.ACTIVE)
                    .build();

            user = userRepository.save(user); // Gán lại user sau khi save để có ID
        } else {
            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE);
            }

            if (!user.getTenant().getId().equals(request.getTenantId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            // Cập nhật thông tin từ Zalo nếu có thay đổi
            boolean changed = false;
            if (request.getFullName() != null && !request.getFullName().equals(user.getFullName())) {
                user.setFullName(request.getFullName());
                changed = true;
            }
            if (request.getAvatar() != null && !request.getAvatar().equals(user.getAvatar())) {
                user.setAvatar(request.getAvatar());
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserSummaryResponse(user))
                .build();
    }

    @Override
    public void requestTwoStepReset(String emailOrPhone) {
        // 1. Tìm User (Admin)
        User user = userRepository.findByUsername(emailOrPhone)
                .or(() -> userRepository.findByEmail(emailOrPhone))
                .or(() -> userRepository.findByPhone(emailOrPhone))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Tenant tenant = user.getTenant();
        if (tenant == null)
            throw new AppException(ErrorCode.TENANT_NOT_FOUND);

        // 2. Tạo 2 mã OTP (6 số ngẫu nhiên)
        String otpCompany = String.valueOf((int) (Math.random() * 900000) + 100000);
        String otpAdmin = String.valueOf((int) (Math.random() * 900000) + 100000);

        // 3. Lưu vào Redis (10 phút)
        String keyComp = "2fa:reset:comp:" + user.getId();
        String keyAdmin = "2fa:reset:admin:" + user.getId();
        redisTemplate.opsForValue().set(keyComp, otpCompany, 10, java.util.concurrent.TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(keyAdmin, otpAdmin, 10, java.util.concurrent.TimeUnit.MINUTES);

        // 4. Gửi Mail xác thực kép
        emailService.sendTwoStepResetEmail(tenant.getEmail(), "DOANH NGHIỆP", otpCompany);
        emailService.sendTwoStepResetEmail(user.getEmail(), "CHỦ SỞ HỮU", otpAdmin);
    }

    @Override
    @Transactional
    public void confirmTwoStepReset(String emailOrPhone, String companyOtp, String adminOtp, String newPassword) {
        User user = userRepository.findByUsername(emailOrPhone)
                .or(() -> userRepository.findByEmail(emailOrPhone))
                .or(() -> userRepository.findByPhone(emailOrPhone))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String keyComp = "2fa:reset:comp:" + user.getId();
        String keyAdmin = "2fa:reset:admin:" + user.getId();

        Object cachedComp = redisTemplate.opsForValue().get(keyComp);
        Object cachedAdmin = redisTemplate.opsForValue().get(keyAdmin);

        if (cachedComp == null || cachedAdmin == null) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        if (!cachedComp.toString().equals(companyOtp) || !cachedAdmin.toString().equals(adminOtp)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Kích hoạt đổi mật khẩu và hạ cờ
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Dọn dẹp
        redisTemplate.delete(keyComp);
        redisTemplate.delete(keyAdmin);
    }

    @Override
    public void forgotPassword(com.example.bezma.dto.req.auth.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        String key = "password_reset:otp:" + user.getEmail();
        redisTemplate.opsForValue().set(key, otp, 10, java.util.concurrent.TimeUnit.MINUTES);

        emailService.sendPasswordResetEmail(user.getEmail(), otp);
    }

    @Override
    @Transactional
    public void resetPassword(com.example.bezma.dto.req.auth.ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        String key = "password_reset:otp:" + request.getEmail();
        Object cachedOtp = redisTemplate.opsForValue().get(key);

        if (cachedOtp == null) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        if (!cachedOtp.toString().equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        redisTemplate.delete(key);
    }
}
