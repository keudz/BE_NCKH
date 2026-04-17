package com.example.bezma.mapper;

import com.example.bezma.dto.res.auth.AuthResponse;
import com.example.bezma.dto.res.user.UserDetailResponse;
import com.example.bezma.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {
    @Mapping(target = "avatarZalo", source = "avatar")
    @Mapping(target = "subPhone", source = "phone")
    UserDetailResponse toUserDetail(User user);

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    AuthResponse toAuthResponse(String accessToken, String refreshToken);
}
