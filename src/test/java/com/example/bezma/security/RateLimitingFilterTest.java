package com.example.bezma.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Unit Tests")
class RateLimitingFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getParameter("token")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("Request bình thường nằm trong giới hạn -> cho qua filter")
    void doFilter_UnderLimit_Proceeds() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/products");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("Request vượt quá giới hạn -> trả về HttpStatus 429")
    void doFilter_ExceedLimit_Returns429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/attendance/check-in");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // 15 requests đầu được đi qua
        for (int i = 0; i < 15; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        verify(filterChain, times(15)).doFilter(request, response);

        // Request thứ 16 bị chặn
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertThat(stringWriter.toString()).contains("429", "Yêu cầu quá nhiều");
    }

    @Test
    @DisplayName("Bỏ qua rate limit cho URL WebSocket và Swagger UI")
    void doFilter_BypassedUrls_AlwaysProceed() throws Exception {
        when(request.getRequestURI()).thenReturn("/ws-notification");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}
