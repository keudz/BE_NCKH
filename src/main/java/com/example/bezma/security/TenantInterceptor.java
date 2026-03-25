package com.example.bezma.security;

import com.example.bezma.config.TenantContext;
import com.example.bezma.exeption.TenantNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = null;

        // Lấy từ Header X-Tenant-ID
        tenantId = request.getHeader("X-Tenant-ID");

        // Nếu không có, lấy từ Authorization Header (JWT)
        if (tenantId == null || tenantId.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tenantId = extractTenantIdFromJwt(token);
            }
        }

        // Nếu vẫn không có
        if (tenantId == null || tenantId.isEmpty()) {
            throw new TenantNotFoundException("Tenant ID identity is missing");
        }

        TenantContext.setCurrentTenant(tenantId);
        return true;
    }

    private String extractTenantIdFromJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("tenantId", String.class);
        } catch (Exception e) {
            // Return null để ném ra TenantNotFoundException
            return null;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
