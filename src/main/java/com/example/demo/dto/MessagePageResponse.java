package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MessagePageResponse(
    List<MessageResponse> messages, // Tin nhắn của trang hiện tại (mới nhất trước)
    LocalDateTime nextCursor,       // createdAt của tin cũ nhất trang này, null nếu đã hết tin
    boolean hasMore,                // Còn tin cũ hơn để tải tiếp hay không
    List<SeenInfo> seenBy           // Các thành viên khác đã xem đến thời điểm nào
) {}
