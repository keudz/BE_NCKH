package com.example.bezma.security;

import io.github.bucket4j.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Bỏ qua các URL tĩnh, WebSocket, Swagger và API docs
        if (requestURI.startsWith("/ws-notification") || 
            requestURI.contains("/favicon.ico") || 
            requestURI.startsWith("/swagger-ui") || 
            requestURI.startsWith("/v3/api-docs") || 
            requestURI.startsWith("/swagger-resources") || 
            requestURI.startsWith("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        boolean isAiPath = requestURI.contains("/api/v1/agent") || 
                           requestURI.contains("/api/v1/attendance/check-in") || 
                           requestURI.contains("/api/v1/attendance/checkout");
        boolean isAuthPath = requestURI.startsWith("/api/auth/") || 
                             requestURI.contains("/public/register") || 
                             requestURI.contains("/public/verify");
        
        String bucketType = isAiPath ? "ai" : (isAuthPath ? "auth" : "normal");
        String cacheKey = key + ":" + bucketType;

        Bucket bucket = cache.computeIfAbsent(cacheKey, k -> createNewBucket(bucketType));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {} on URI: {}", key, requestURI);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\": 429, \"message\": \"Yêu cầu quá nhiều! Vui lòng thử lại sau ít phút.\", \"timestamp\": " 
                    + System.currentTimeMillis() + "}"
            );
        }
    }

    private Bucket createNewBucket(String bucketType) {
        Refill refill;
        Bandwidth limit;
        if ("ai".equals(bucketType)) {
            // Nhận diện khuôn mặt và chatbot: tối đa 15 requests mỗi phút
            refill = Refill.intervally(15, Duration.ofMinutes(1));
            limit = Bandwidth.classic(15, refill);
        } else if ("auth".equals(bucketType)) {
            // Xác thực, đăng ký, đăng nhập: tối đa 20 requests mỗi phút
            refill = Refill.intervally(20, Duration.ofMinutes(1));
            limit = Bandwidth.classic(20, refill);
        } else {
            // Các API bình thường khác: tối đa 60 requests mỗi phút
            refill = Refill.intervally(60, Duration.ofMinutes(1));
            limit = Bandwidth.classic(60, refill);
        }
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String resolveKey(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                if (claims != null && jwtTokenProvider.validateToken(jwt)) {
                    Long userId = claims.get("userId", Long.class);
                    if (userId != null) {
                        return "user:" + userId;
                    }
                    String username = claims.getSubject();
                    if (username != null) {
                        return "user:" + username;
                    }
                }
            } catch (Exception e) {
                // Fallback về IP nếu token hết hạn hoặc lỗi
            }
        }
        return "ip:" + getClientIp(request);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
