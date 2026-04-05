package com.example.bezma.security;

// import com.example.bezma.security.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.example.bezma.entity.user.User;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor // Dùng cái này thay cho @Autowired field để code sạch hơn
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private final CustomUserDetailsService userDetailsService;

    // ---- Tạo token ----
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("tenantId", user.getTenant().getId())
                .claim("role", user.getRole().getName()) // Thêm role name cho dễ check ở FE
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    // ---- Tạo Refresh Token ----
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("tenantId", user.getTenant().getId())
                .claim("role", user.getRole().getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    // ---- Lấy Authentication (Fix lỗi Cast Class) ----
    public Authentication getAuthentication(String token) {
        try {
            String username = getUsernameFromToken(token); // Dùng hàm đồng nhất ở dưới

            // QUAN TRỌNG: Load UserDetails xịn từ DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Trả về Token với Principal là UserDetails (User base)
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Đổi tên cho khớp với Filter (getUsernameFromToken) ----
    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Validate token ----
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Chữ ký JWT không hợp lệ: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token không đúng định dạng: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token đã hết hạn: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims trống hoặc không hợp lệ: {}", e.getMessage());
        }
        return false;
    }

    // Lấy userId nếu cần dùng nhanh
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getTenantIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("tenantId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }
}