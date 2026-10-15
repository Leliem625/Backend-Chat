package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.ConversationType;

public record ConversationResponse(
    Long id,
    ConversationType type,
    String title,               // Tên bạn chat 1-1 HOẶC tên nhóm
    String avatarUrl,           // Avatar bạn chat 1-1 HOẶC avatar nhóm
    String lastMessage,         // Nội dung tin nhắn cuối cùng preview
    Long lastMessageSenderId,   // ID người gửi tin nhắn cuối cùng
    LocalDateTime lastMessageAt,// Thời gian của tin nhắn cuối cùng
    Integer unreadCount,        // Số tin nhắn chưa đọc của người dùng hiện tại
    Long createdBy,             // ID người tạo nhóm / Quản trị viên chính
    Long otherUserId,           // Chat 1-1: id của đối phương (nhóm: null)
    BlockStatus blockStatus     // Chat 1-1: trạng thái chặn -> frontend khoá ô nhập (nhóm: NONE)
) {
    // Cho cuộc trò chuyện nhóm: không có đối phương, không có trạng thái chặn
    public ConversationResponse(Long id, ConversationType type, String title, String avatarUrl,
            String lastMessage, Long lastMessageSenderId, LocalDateTime lastMessageAt,
            Integer unreadCount, Long createdBy) {
        this(id, type, title, avatarUrl, lastMessage, lastMessageSenderId, lastMessageAt,
                unreadCount, createdBy, null, BlockStatus.NONE);
    }
}
