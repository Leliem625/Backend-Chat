package com.example.demo.dto;

// App di động gửi refresh token qua body (web dùng cookie)
public record RefreshTokenRequest(String refreshToken) {}
