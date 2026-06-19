package com.example.bezma.service;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.auth.LoginRequest;
import com.example.bezma.dto.req.auth.RefreshTokenRequest;
import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.security.JwtTokenProvider;
import com.example.bezma.service.impl.AuthServiceImpl;
import com.example.bezma.service.impl.ZaloServiceImpl;
import com.example.bezma.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private ZaloServiceImpl zaloService;
    @Mock private RoleRepository roleRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Tenant testTenant;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .name("Test Company")
                .tenantCode("TC001")
                .slug("test-company")
                .phone("0123456789")
                .email("company@test.com")
                .build();

        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("ADMIN");

        testUser = User.builder()
                .id(1L)
                .username("admin")
                .password("encodedPassword")
                .fullName("Admin User")
                .email("admin@test.com")
                .phone("0987654321")
                .tenant(testTenant)
                .role(testRole)
                .isActive(true)
                .mustChangePassword(false)
                .build();
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công → trả về token và user info")
        void login_Success_ReturnsAuthResponse() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token-123");
            when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("refresh-token-456");

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-123");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUsername()).isEqualTo("admin");
            assertThat(response.getUser().getFullName()).isEqualTo("Admin User");
            assertThat(response.getUser().getTenantId()).isEqualTo(1L);

            verify(authenticationManager).authenticate(any());
            verify(jwtTokenProvider).generateAccessToken(testUser);
            verify(jwtTokenProvider).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Đăng nhập sai mật khẩu → throw AppException VALIDATE_LOGIN")
        void login_BadCredentials_ThrowsAppException() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("wrongPassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.login(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATE_LOGIN);
        }

        @Test
        @DisplayName("Đăng nhập tài khoản bị khóa → throw AppException USER_NOT_ACTIVE")
        void login_InactiveUser_ThrowsAppException() {
            // Arrange
            testUser.setIsActive(false);

            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.login(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_ACTIVE);
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh token hợp lệ → trả về token mới")
        void refreshToken_ValidToken_ReturnsNewTokens() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

            // Act
            AuthResponse response = authService.refreshToken(request);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.getUser().getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Refresh token hết hạn → throw RuntimeException")
        void refreshToken_ExpiredToken_ThrowsException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired-token");

            when(jwtTokenProvider.validateToken("expired-token")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Refresh Token không hợp lệ");
        }

        @Test
        @DisplayName("Refresh token cho user bị khóa → throw RuntimeException")
        void refreshToken_InactiveUser_ThrowsException() {
            // Arrange
            testUser.setIsActive(false);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("bị vô hiệu hóa");
        }
    }

    // ==================== FORGOT PASSWORD TESTS ====================

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Quên mật khẩu với email hợp lệ → gửi OTP qua email")
        void forgotPassword_ValidEmail_SendsOtp() {
            // Arrange
            com.example.bezma.dto.req.auth.ForgotPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ForgotPasswordRequest();
            request.setEmail("admin@test.com");

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // Act
            authService.forgotPassword(request);

            // Assert
            verify(valueOperations).set(
                    eq("password_reset:otp:admin@test.com"),
                    anyString(),
                    eq(10L),
                    eq(TimeUnit.MINUTES));
            verify(emailService).sendPasswordResetEmail(eq("admin@test.com"), anyString());
        }

        @Test
        @DisplayName("Quên mật khẩu với email không tồn tại → throw AppException")
        void forgotPassword_InvalidEmail_ThrowsException() {
            // Arrange
            com.example.bezma.dto.req.auth.ForgotPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ForgotPasswordRequest();
            request.setEmail("nonexistent@test.com");

            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.forgotPassword(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ==================== RESET PASSWORD TESTS ====================

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Reset mật khẩu không khớp confirm → throw PASSWORD_NOT_MATCH")
        void resetPassword_PasswordMismatch_ThrowsException() {
            // Arrange
            com.example.bezma.dto.req.auth.ResetPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ResetPasswordRequest();
            request.setEmail("admin@test.com");
            request.setOtp("123456");
            request.setNewPassword("newPassword1");
            request.setConfirmPassword("differentPassword");

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.resetPassword(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_NOT_MATCH);
        }

        @Test
        @DisplayName("Reset mật khẩu OTP hết hạn → throw TOKEN_EXPIRED")
        void resetPassword_ExpiredOtp_ThrowsException() {
            // Arrange
            com.example.bezma.dto.req.auth.ResetPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ResetPasswordRequest();
            request.setEmail("admin@test.com");
            request.setOtp("123456");
            request.setNewPassword("newPassword1");
            request.setConfirmPassword("newPassword1");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:otp:admin@test.com")).thenReturn(null);

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.resetPassword(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("Reset mật khẩu OTP sai → throw INVALID_KEY")
        void resetPassword_WrongOtp_ThrowsException() {
            // Arrange
            com.example.bezma.dto.req.auth.ResetPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ResetPasswordRequest();
            request.setEmail("admin@test.com");
            request.setOtp("999999");
            request.setNewPassword("newPassword1");
            request.setConfirmPassword("newPassword1");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:otp:admin@test.com")).thenReturn("123456");

            // Act & Assert
            AppException exception = catchThrowableOfType(
                    () -> authService.resetPassword(request), AppException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_KEY);
        }

        @Test
        @DisplayName("Reset mật khẩu thành công → cập nhật password và xóa OTP")
        void resetPassword_Success_UpdatesPassword() {
            // Arrange
            com.example.bezma.dto.req.auth.ResetPasswordRequest request =
                    new com.example.bezma.dto.req.auth.ResetPasswordRequest();
            request.setEmail("admin@test.com");
            request.setOtp("123456");
            request.setNewPassword("newPassword1");
            request.setConfirmPassword("newPassword1");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:otp:admin@test.com")).thenReturn("123456");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword1")).thenReturn("encodedNewPassword");

            // Act
            authService.resetPassword(request);

            // Assert
            assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
            assertThat(testUser.getMustChangePassword()).isFalse();
            verify(userRepository).save(testUser);
            verify(redisTemplate).delete("password_reset:otp:admin@test.com");
        }
    }
}
