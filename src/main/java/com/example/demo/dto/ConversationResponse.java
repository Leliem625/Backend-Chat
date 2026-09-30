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
    Long createdBy              // ID người tạo nhóm / Quản trị viên chính
) {}
