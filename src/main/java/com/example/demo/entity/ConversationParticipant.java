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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
        // Đảm bảo mỗi user chỉ tham gia 1 lần vào 1 cuộc trò chuyện
        @UniqueConstraint(name = "uk_conversation_user", columnNames = { "conversation_id", "user_id" })
}, indexes = {
        // Index để lấy nhanh danh sách các cuộc trò chuyện mà một user tham gia
        @Index(name = "idx_user_conversation", columnList = "user_id, conversation_id")
})
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Vai trò trong cuộc trò chuyện (ADMIN: Quản trị viên, MEMBER: Thành viên)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ParticipantRole role = ParticipantRole.MEMBER;

    // Tương đương joinedAt: { type: Date, default: Date.now }
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    // Tương đương unreadCounts: { userId: Number }
    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    // Tương đương seenBy: [userId] (Thời điểm user này xem tin nhắn cuối)
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public ConversationParticipant() {
    }

    public ConversationParticipant(Long conversationId, Long userId) {
        this(conversationId, userId, ParticipantRole.MEMBER);
    }

    public ConversationParticipant(Long conversationId, Long userId, ParticipantRole role) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role != null ? role : ParticipantRole.MEMBER;
        this.unreadCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = ParticipantRole.MEMBER;
        }
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    @Override
    public String toString() {
        return "ConversationParticipant{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", userId=" + userId +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                ", unreadCount=" + unreadCount +
                ", lastSeenAt=" + lastSeenAt +
                '}';
    }
}
