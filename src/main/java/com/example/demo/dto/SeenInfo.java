package com.example.demo.dto;

import java.time.LocalDateTime;

// Thời điểm 1 thành viên xem cuộc trò chuyện lần cuối, để client hiện avatar "đã xem"
public record SeenInfo(
    Long userId,
    String avatarUrl,
    LocalDateTime lastSeenAt
) {}
