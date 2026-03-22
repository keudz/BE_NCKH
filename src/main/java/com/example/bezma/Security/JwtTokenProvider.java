package com.example.demo.Security;

import com.example.bezma.Security.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.example.demo.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor // Dùng cái này thay cho @Autowired field để code sạch hơn
public class JwtTokenProvider {

    private static final String SECRET_KEY = "bXktc3VwZXItc2VjcmV0LXN1cGVyLXNlY3JldC1rZXktMTIzNDU2Nzg5MA==";
    private final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    @Value("${jwt.expiration}") // Lấy từ application.properties
    private long jwtExpirationMs;
    private final CustomUserDetailsService userDetailsService;

    // ---- Tạo token ----
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().getName()) // Thêm role name cho dễ check ở FE
                .claim("authorities", authorities)
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

            // Trả về Token với Principal là UserDetails (User entity)
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
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
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
}