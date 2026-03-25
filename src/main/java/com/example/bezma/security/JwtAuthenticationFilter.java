package com.example.bezma.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final com.example.demo.Security.JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Bỏ qua log cho các request favicon để đỡ rối log
        if (!requestURI.contains("/favicon.ico")) {
            log.info("Processing request: {} {}", request.getMethod(), requestURI);
        }

        try {
            // Lấy JWT từ Header (Zalo) hoặc Cookie (Web)
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // getAuthentication này giờ đã load UserDetails (Entity User) từ DB
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

                if (authentication != null) {
                    // Lưu cả Object User vào Context, không còn bị lỗi ClassCastException nữa
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("Authenticated user: {}", authentication.getName());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Check Header (Ưu tiên hàng đầu)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Check Cookies (Dành cho FE gọi API từ trình duyệt)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. Check Query Param (Dùng cho link ảnh/download nếu cần)
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }
}