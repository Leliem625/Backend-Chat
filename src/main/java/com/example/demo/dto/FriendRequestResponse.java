package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.FriendRequest;
import com.example.demo.entity.User;

// 1 lời mời kết bạn mình nhận được, kèm thông tin công khai của người gửi (KHÔNG trả email)
public record FriendRequestResponse(
    Long id,              // id lời mời -> dùng cho "Chấp nhận" / "Từ chối"
    Long fromUserId,
    String username,
    String avatarUrl,
    String currentCity,
    String message,
    LocalDateTime createdAt
) {
    public static FriendRequestResponse from(FriendRequest request, User from) {
        return new FriendRequestResponse(
            request.getId(),
            request.getFromUserId(),
            from != null ? from.getUsername() : null,
            from != null ? from.getAvatarUrl() : null,
            from != null ? from.getCurrentCity() : null,
            request.getMessage(),
            request.getCreatedAt()
        );
    }
}
