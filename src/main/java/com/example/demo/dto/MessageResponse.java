package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.entity.MessageType;
import com.example.demo.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
    Long id,
    Long conversationId,
    Long senderId,
    UserResponse sender,
    String content,
    MessageType type,
    List<AttachmentResponse> attachments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AttachmentResponse(
        Long id,
        String url,
        String fileName,
        String resourceType,
        String mimeType,
        Long size
    ) {
        public static AttachmentResponse fromEntity(MessageAttachment attachment) {
            if (attachment == null) {
                return null;
            }
            return new AttachmentResponse(
                attachment.getId(),
                attachment.getUrl(),
                attachment.getFileName(),
                attachment.getResourceType(),
                attachment.getMimeType(),
                attachment.getSize()
            );
        }
    }

    // Helper method tạo từ Message entity đơn giản
    public static MessageResponse fromMessage(Message message) {
        if (message == null) {
            return null;
        }
        return new MessageResponse(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            null,
            message.getContent(),
            message.getType(),
            null,
            message.getCreatedAt(),
            message.getUpdatedAt()
        );
    }

    // Helper method tạo từ Message kèm UserResponse và danh sách AttachmentResponse
    public static MessageResponse fromMessage(
            Message message,
            UserResponse sender,
            List<AttachmentResponse> attachments) {
        if (message == null) {
            return null;
        }
        return new MessageResponse(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            sender,
            message.getContent(),
            message.getType(),
            attachments,
            message.getCreatedAt(),
            message.getUpdatedAt()
        );
    }

    // Helper method tiện lợi: tự động convert User và List<MessageAttachment>
    public static MessageResponse fromMessage(
            Message message,
            User sender,
            List<MessageAttachment> attachments) {
        if (message == null) {
            return null;
        }
        UserResponse senderResponse = sender != null ? UserResponse.fromUser(sender) : null;
        List<AttachmentResponse> attachmentResponses = attachments != null
                ? attachments.stream().map(AttachmentResponse::fromEntity).toList()
                : null;
        return fromMessage(message, senderResponse, attachmentResponses);
    }

    // --- Các hàm getter tương thích nếu quen gọi kiểu get...() ---
    public Long getId() { return id(); }
    public Long getConversationId() { return conversationId(); }
    public Long getSenderId() { return senderId(); }
    public UserResponse getSender() { return sender(); }
    public String getContent() { return content(); }
    public MessageType getType() { return type(); }
    public List<AttachmentResponse> getAttachments() { return attachments(); }
    public LocalDateTime getCreatedAt() { return createdAt(); }
    public LocalDateTime getUpdatedAt() { return updatedAt(); }
}
