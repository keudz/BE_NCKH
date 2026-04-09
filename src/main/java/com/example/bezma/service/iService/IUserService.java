package com.example.bezma.service.iService;

import com.example.bezma.dto.res.user.UserSummaryResponse;

import java.util.List;

public interface IUserService {
    UserSummaryResponse getMyProfile();
    List<UserSummaryResponse> getAllUsersInMyTenant();
}