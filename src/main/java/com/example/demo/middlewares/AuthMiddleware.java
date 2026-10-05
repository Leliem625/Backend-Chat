package com.example.demo.middlewares;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthMiddleware extends OncePerRequestFilter {
    private final JwtService jwtService;

    public AuthMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return;
        }
        String token = authHeader.substring(7);
        try {
            // Cho phép request đi tiếp tới Controller tiếp theo (tương đương next()):
            Long userId = jwtService.extractUserId(token);
            request.setAttribute("userId", userId);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Cho phép tất cả request OPTIONS (CORS preflight) đi qua
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // 1. Danh sách các route PUBLIC (cho qua, không cần token):
        if (path.equals("/") ||
                path.equals("/api/auth/login") || path.equals("/api/auth/login/") ||
                path.equals("/api/auth/register") || path.equals("/api/auth/register/") ||
                path.equals("/api/auth/refresh-token") || path.equals("/api/auth/refresh-token/") ||
                path.equals("/api/auth/send-otp") || path.equals("/api/auth/send-otp/") ||
                path.equals("/api/auth/forgot-password") || path.equals("/api/auth/forgot-password/") ||
                path.equals("/api/auth/verify-otp") || path.equals("/api/auth/verify-otp/")) {
            return true; // Bỏ qua không filter
        }

        return false;
    }

}
