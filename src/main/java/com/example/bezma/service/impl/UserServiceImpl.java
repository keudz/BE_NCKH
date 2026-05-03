package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.RoleRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.CloudinaryService;
import com.example.bezma.service.iService.IUserService;
import com.example.bezma.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        // Fallback logic if principal is just a string (should not happen with current JWT setup)
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.append("@").toString();
    }

    @Override
    @Transactional
    public UserSummaryResponse createUser(UserCreateRequest request) {
        User admin = getCurrentUser();

        log.info("Creating new user with email: {}", request.getEmail());

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String roleName = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles().get(0)
                : "STAFF";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        // Dùng UUID làm mật khẩu tạm thời để tránh null
        String tempPassword = UUID.randomUUID().toString();
        
        User newUser = User.builder()
                .username(request.getPhone())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(role)
                .tenant(admin.getTenant())
                .isActive(false)
                .status(UserStatus.PENDING_ACTIVE)
                .verificationToken(UUID.randomUUID().toString())
                .verificationTokenExpiry(LocalDateTime.now().plusDays(7))
                .gender(request.getGender())
                .address(request.getAddress())
                .identityCard(request.getIdentityCard())
                .mustChangePassword(true)
                .build();

        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            try {
                newUser.setBirthday(LocalDateTime.parse(request.getBirthday() + "T00:00:00"));
            } catch (Exception e) {
                log.warn("Invalid birthday format: {}", request.getBirthday());
            }
        }

        newUser.setIsDeleted(false);
        userRepository.save(newUser);

        // Gửi email mời
        emailService.sendEmployeeInvitation(newUser.getEmail(), newUser.getFullName(), newUser.getVerificationToken());

        return mapToResponse(newUser);
    }

    @Override
    @Transactional
    public void activateUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        String rawPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setIsActive(true);
        user.setIsVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        
        userRepository.save(user);

        // Gửi email mật khẩu
        emailService.sendEmployeeCredentials(user.getEmail(), user.getFullName(), user.getUsername(), rawPassword);
    }

    @Override
    public UserSummaryResponse getMyProfile() {
        User user = getCurrentUser();

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateMyProfile(UserUpdateRequest request) {
        User user = getCurrentUser();
        updateUserDetails(user, request);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Override
    public List<UserSummaryResponse> getAllUsersInMyTenant(Boolean isDeleted) {
        User currentUser = getCurrentUser();
        Long myTenantId = currentUser.getTenant().getId();

        Boolean filterDeleted = (isDeleted != null) ? isDeleted : false;
        List<User> users = userRepository.findAllByTenantIdAndIsDeleted(myTenantId, filterDeleted);

        return users.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long pathId, UserUpdateRequest request) {
        Long targetUserId = (pathId != null) ? pathId : request.getId();
        User admin = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        updateUserDetails(targetUser, request);
        userRepository.save(targetUser);

        return mapToResponse(targetUser);
    }

    private void updateUserDetails(User user, UserUpdateRequest request) {
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getIdentityCard() != null) user.setIdentityCard(request.getIdentityCard());
        
        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            try {
                user.setBirthday(LocalDateTime.parse(request.getBirthday() + "T00:00:00"));
            } catch (Exception e) {
                log.warn("Invalid birthday format: {}", request.getBirthday());
            }
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }
            user.setPhone(request.getPhone());
            user.setUsername(request.getPhone());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
        }
    }

    private UserSummaryResponse mapToResponse(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .isActive(user.getIsActive())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .birthday(user.getBirthday() != null ? user.getBirthday().format(formatter) : null)
                .gender(user.getGender())
                .address(user.getAddress())
                .identityCard(user.getIdentityCard())
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId) {
        User admin = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (admin.getId().equals(targetUser.getId())) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        targetUser.setIsActive(false);
        targetUser.setIsDeleted(true);
        targetUser.setStatus(UserStatus.DELETED);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public void restoreUser(Long targetUserId) {
        User admin = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        targetUser.setIsDeleted(false);
        targetUser.setIsActive(true);
        targetUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("User {} changed password successfully and reset mustChangePassword flag.", user.getUsername());
    }

    @Override
    @Transactional
    public UserSummaryResponse updateAvatar(MultipartFile file) {
        User user = getCurrentUser();
        
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        String folder = "users/" + user.getTenant().getId() + "/avatars";
        String avatarUrl = cloudinaryService.uploadFile(file, folder);
        
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        
        log.info("User {} updated avatar successfully: {}", user.getUsername(), avatarUrl);
        
        return mapToResponse(user);
    }
}