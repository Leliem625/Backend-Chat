package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret:123456}")
    private String secretKey;

    @Value("${jwt.expiration:60000}")
    private long jwtExpirationMs; // 86400000 ms = 24 giờ

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs; // 604800000 ms = 7 ngày

    private SecretKey getSigningKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán SHA-256", e);
        }
    }

    /**
     * Tạo Access Token từ userId (hạn 24 giờ)
     */
    public String generateAccessToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Tạo Refresh Token từ userId (hạn 7 ngày)
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Lấy userId từ Access Token
     */
    public Long extractUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    /**
     * Kiểm tra token có hợp lệ với userId và còn hạn không
     */
    public boolean validateToken(String token, Long userId) {
        try {
            return extractUserId(token).equals(userId) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra token đã hết hạn chưa
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
