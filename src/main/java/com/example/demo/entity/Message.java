package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "messages",
    indexes = {
        // Lấy lịch sử tin nhắn của 1 cuộc trò chuyện, phân trang theo id (id tự tăng nên luôn đúng thứ tự)
        @Index(name = "idx_conversation_id_id", columnList = "conversation_id, id")
    }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tương đương conversationId: { type: ObjectId, ref: 'Conversation', required: true }
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    // Tương đương senderId: { type: ObjectId, ref: 'User', required: true }
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // Tương đương content: { type: String, trim: true }
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // TEXT (văn bản), IMAGE (ảnh), FILE (tệp), SYSTEM (tin hệ thống)
    // Ảnh / tệp đính kèm lưu ở bảng message_attachments
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type = MessageType.TEXT;

    // Tương đương timestamps: true
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Message() {
    }

    public Message(Long conversationId, Long senderId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
    }

    public Message(Long conversationId, Long senderId, String content, MessageType type) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.type == null) {
            this.type = MessageType.TEXT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getter và Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
