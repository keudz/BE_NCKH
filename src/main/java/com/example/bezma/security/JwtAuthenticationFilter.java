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
import com.example.bezma.util.TenantContext;
import io.jsonwebtoken.Claims;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/ws-notification")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!requestURI.contains("/favicon.ico")) {
            log.info("Processing request: {} {}", request.getMethod(), requestURI);
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                
                if (claims != null && jwtTokenProvider.validateToken(jwt)) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(jwt, claims);

                    if (authentication != null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        Long userId = claims.get("userId", Long.class);
                        if (userId != null) {
                            request.setAttribute("userId", userId);
                        }

                        Long tenantId = claims.get("tenantId", Long.class);
                        if (tenantId != null) {
                            TenantContext.setCurrentTenantId(tenantId);
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
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
}