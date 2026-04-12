package com.example.bezma.service.iService;

import com.example.bezma.dto.req.user.UserUpdateRequest;
import com.example.bezma.dto.res.user.UserSummaryResponse;

import java.util.List;

public interface IUserService {
    UserSummaryResponse getMyProfile();
    List<UserSummaryResponse> getAllUsersInMyTenant(Boolean isDeleted);
    UserSummaryResponse updateUser(Long pathId, UserUpdateRequest request);
    void deleteUser(Long targetUserId);
    void restoreUser(Long targetUserId);
}