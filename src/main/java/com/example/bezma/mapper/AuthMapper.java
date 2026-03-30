package com.example.bezma.mapper;

import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.dto.res.user.UserDetailResponse;
import com.example.bezma.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserDetailResponse toUserDetail(User user);

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    AuthResponse toAuthResponse(String accessToken, String refreshToken);
}
