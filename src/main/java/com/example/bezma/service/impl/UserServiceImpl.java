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
import com.example.bezma.service.iService.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public UserSummaryResponse createUser(UserCreateRequest request) {
        User admin = getCurrentUser();

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

        User newUser = User.builder()
                .username(request.getPhone())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .tenant(admin.getTenant())
                .isActive(true)
                .status(UserStatus.ACTIVE)
                .build();

        newUser.setIsDeleted(false);
        userRepository.save(newUser);

        return UserSummaryResponse.builder()
                .id(newUser.getId())
                .username(newUser.getUsername())
                .fullName(newUser.getFullName())
                .email(newUser.getEmail())
                .phone(newUser.getPhone())
                .isActive(newUser.getIsActive())
                .roleName(role.getName())
                .tenantId(admin.getTenant().getId())
                .build();
    }

    @Override
    public UserSummaryResponse getMyProfile() {
        User user = getCurrentUser();

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

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
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    @Override
    @Transactional
    public UserSummaryResponse updateMyProfile(UserUpdateRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userRepository.save(user);

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
                .build();
    }

    @Override
    public List<UserSummaryResponse> getAllUsersInMyTenant(Boolean isDeleted) {
        User currentUser = getCurrentUser();
        Long myTenantId = currentUser.getTenant().getId();

        Boolean filterDeleted = (isDeleted != null) ? isDeleted : false;
        List<User> users = userRepository.findAllByTenantIdAndIsDeleted(myTenantId, filterDeleted);

        return users.stream().map(user -> UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .tenantId(myTenantId)
                .build()).toList();
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long pathId, UserUpdateRequest request) {
        Long targetUserId = (pathId != null) ? pathId : request.getId();
        if (targetUserId == null) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        User admin = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getFullName() != null) {
            targetUser.setFullName(request.getFullName());
        }

        if (request.getAvatar() != null) {
            targetUser.setAvatar(request.getAvatar());
        }

        if (request.getPhone() != null && !request.getPhone().equals(targetUser.getPhone())) {
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }
            targetUser.setPhone(request.getPhone());
            targetUser.setUsername(request.getPhone());
        }

        if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            targetUser.setEmail(request.getEmail());
        }

        userRepository.save(targetUser);

        return UserSummaryResponse.builder()
                .id(targetUser.getId())
                .username(targetUser.getUsername())
                .fullName(targetUser.getFullName())
                .email(targetUser.getEmail())
                .phone(targetUser.getPhone())
                .avatar(targetUser.getAvatar())
                .isActive(targetUser.getIsActive())
                .roleName(targetUser.getRole() != null ? targetUser.getRole().getName() : null)
                .tenantId(targetUser.getTenant() != null ? targetUser.getTenant().getId() : null)
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
}