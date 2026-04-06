package com.example.bezma.service.impl;

import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IUserService; // Thêm import này
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService { // Sửa dòng này

    private final UserRepository userRepository;

    @Override // Thêm dòng này
    public UserSummaryResponse getMyProfile() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng!"));

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
}