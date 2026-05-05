package com.example.bezma.service.iService;

import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.dto.res.user.UserSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUserService {
    UserSummaryResponse getMyProfile();
    UserSummaryResponse updateMyProfile(UserUpdateRequest request);

    List<UserSummaryResponse> getAllUsersInMyTenant(Boolean isDeleted);

    UserSummaryResponse createUser(UserCreateRequest request);

    UserSummaryResponse updateUser(Long pathId, UserUpdateRequest request);

    void deleteUser(Long targetUserId);

    void restoreUser(Long targetUserId);

    void requestChangePasswordOTP();
    void changePassword(String oldPassword, String newPassword, String otp);
    void activateUser(String token);
    
    UserSummaryResponse updateAvatar(MultipartFile file);
}