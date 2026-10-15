package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.User;
import com.example.demo.entity.UserBlock;

// 1 dòng trong danh sách "Người đã chặn": chỉ thông tin công khai (KHÔNG trả email, phone)
public record BlockedUserResponse(
    Long id,                 // id của người bị chặn -> dùng cho nút "Bỏ chặn"
    String username,
    String avatarUrl,
    LocalDateTime blockedAt  // chặn từ lúc nào
) {
    public static BlockedUserResponse from(UserBlock block, User blockedUser) {
        return new BlockedUserResponse(
            block.getBlockedId(),
            blockedUser != null ? blockedUser.getUsername() : null,
            blockedUser != null ? blockedUser.getAvatarUrl() : null,
            block.getCreatedAt()
        );
    }
}
