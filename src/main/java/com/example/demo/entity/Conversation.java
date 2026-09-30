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
@Table(name = "conversations", indexes = {
        // Index tối ưu khi sắp xếp danh sách cuộc trò chuyện theo tin nhắn mới nhất
        @Index(name = "idx_last_message_at", columnList = "last_message_at DESC")
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DIRECT (chat 1-1) hoặc GROUP (chat nhóm)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    // Tên cuộc trò chuyện (Nếu là Group thì lưu tên nhóm; nếu 1-1 có thể để null
    // hoặc đặt tên tùy ý)
    @Column(name = "conversation_name")
    private String conversationName;

    // Người tạo cuộc trò chuyện (ID của user khởi tạo)
    @Column(name = "created_by")
    private Long createdBy;

    // --- Thông tin Tin nhắn cuối cùng (Last Message Preview) ---
    @Column(name = "last_message_id")
    private String lastMessageId;

    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // --- Timestamps ---
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Conversation() {
    }

    public Conversation(ConversationType type) {
        this.type = type;
    }

    public Conversation(ConversationType type, String conversationName, Long createdBy) {
        this.type = type;
        this.conversationName = conversationName;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public Long getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(Long lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", type=" + type +
                ", conversationName='" + conversationName + '\'' +
                ", createdBy=" + createdBy +
                ", lastMessageContent='" + lastMessageContent + '\'' +
                ", lastMessageAt=" + lastMessageAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
