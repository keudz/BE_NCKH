package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.exception.AppException;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    @Override
    public UserSummaryResponse getMyProfile() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
                .build();
    }
    @Override
    public List<UserSummaryResponse> getAllUsersInMyTenant() {

        // 1. Xem ai đang gọi API này (Lấy SĐT hoặc Username từ Token)
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByPhone(currentIdentifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Long myTenantId = currentUser.getTenant().getId();

        List<User> users = userRepository.findAllByTenantId(myTenantId);

        return users.stream().map(user -> UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .isActive(user.getIsActive())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .tenantId(myTenantId)
                .build()
        ).toList();
    }
}