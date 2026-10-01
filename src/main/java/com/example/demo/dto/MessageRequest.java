package com.example.demo.dto;

import com.example.demo.entity.MessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MessageRequest(
    @NotNull(message = "conversationId không được để trống")
    Long conversationId,

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    String content,

    MessageType type
) {
    public MessageRequest {
        if (type == null) {
            type = MessageType.TEXT;
        }
    }

    // Constructor tiện lợi cho tin nhắn văn bản
    public MessageRequest(Long conversationId, String content) {
        this(conversationId, content, MessageType.TEXT);
    }

    // Getter tương thích kiểu JavaBean
    public Long getConversationId() { return conversationId(); }
    public String getContent() { return content(); }
    public MessageType getType() { return type(); }
}
