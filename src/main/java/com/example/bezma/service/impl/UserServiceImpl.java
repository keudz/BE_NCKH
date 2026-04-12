package com.example.bezma.service.impl;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.entity.user.UserStatus;
import com.example.bezma.exception.AppException;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<UserSummaryResponse> getAllUsersInMyTenant(Boolean isDeleted) {

        // 1. Xem ai đang gọi API này (Lấy SĐT hoặc Username từ Token)
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByPhone(currentIdentifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Long myTenantId = currentUser.getTenant().getId();

        // 2. Lấy danh sách dựa trên isDeleted (mặc định false nếu null)
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
                .build()
        ).toList();
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long pathId, UserUpdateRequest request) {

        // 1. Xác định ID người cần sửa (Lấy từ URL, nếu URL null thì lấy từ trong Body DTO)
        Long targetUserId = (pathId != null) ? pathId : request.getId();
        if (targetUserId == null) {
            throw new AppException(ErrorCode.INVALID_MESSAGE); // Báo lỗi nếu thiếu ID
        }

        // 2. Lấy thông tin Admin đang gọi API (Dùng số điện thoại vì bạn đã đổi sang SĐT)
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByPhone(currentIdentifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 3. Lấy thông tin Nhân viên đang bị sửa
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 🚨 CHỐT BẢO MẬT (MULTI-TENANT): Khác công ty thì báo lỗi 403 ngay!
        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 4. Cập nhật các trường dữ liệu theo đúng DTO của bạn
        if (request.getFullName() != null) {
            targetUser.setFullName(request.getFullName());
        }

        if (request.getAvatar() != null) {
            targetUser.setAvatar(request.getAvatar());
        }

        // 🚨 Kiểm tra đổi Số điện thoại
        if (request.getPhone() != null && !request.getPhone().equals(targetUser.getPhone())) {
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS); // Đã có người dùng số này
            }
            targetUser.setPhone(request.getPhone());
            targetUser.setUsername(request.getPhone()); // Đồng bộ SĐT vào Username
        }

        // 🚨 Kiểm tra đổi Email
        if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS); // Bắt đúng lỗi 1006 của Lead
            }
            targetUser.setEmail(request.getEmail());
        }

        // 5. Lưu vào Database
        userRepository.save(targetUser);

        // 6. Trả về thông tin mới nhất
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

        // 1. Lấy thông tin Admin đang gọi API
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByPhone(currentIdentifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Lấy thông tin Nhân viên sắp bị bế đi
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        //  CHỐT 1 (MULTI-TENANT): Khác công ty thì báo lỗi 403
        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        //  CHỐT 2: Admin không được tự tay bóp dái (xóa chính mình)
        if (admin.getId().equals(targetUser.getId())) {
            throw new AppException(ErrorCode.INVALID_MESSAGE);
        }

        // 3. THỰC HIỆN XÓA MỀM (Soft Delete) theo logic is_deleted = 1
        targetUser.setIsActive(false);
        targetUser.setIsDeleted(true);
        targetUser.setStatus(UserStatus.DELETED);

        // 4. Lưu lại sự thay đổi
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public void restoreUser(Long targetUserId) {
        String currentIdentifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByPhone(currentIdentifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Khôi phục
        targetUser.setIsDeleted(false);
        targetUser.setIsActive(true);
        targetUser.setStatus(UserStatus.ACTIVE);

        userRepository.save(targetUser);
    }


}