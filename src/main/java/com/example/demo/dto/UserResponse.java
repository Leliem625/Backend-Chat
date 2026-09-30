package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
    private String accessToken;
    private String refreshToken;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, String phone, String avatarUrl, String bio,
            LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
        this(id, username, email, null, null, null, createdAt);
    }

    // Helper method: chuyển đổi từ User Entity sang UserResponse
    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCreatedAt());
    }

    // Helper method kèm Access Token và Refresh Token khi đăng nhập
    public static UserResponse fromUser(User user, String accessToken, String refreshToken) {
        UserResponse response = fromUser(user);
        if (response != null) {
            response.accessToken = accessToken;
            response.refreshToken = refreshToken;
        }
        return response;
    }

    // Helper method kèm Access Token khi đăng nhập
    public static UserResponse fromUser(User user, String accessToken) {
        return fromUser(user, accessToken, null);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // Các hàm Getter để Spring Boot (Jackson) đọc dữ liệu tạo JSON
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
