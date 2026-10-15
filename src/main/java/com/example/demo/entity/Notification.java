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

// Chỉ lưu "ai làm gì với ai", KHÔNG lưu câu chữ / tên người:
// tên + avatar lấy lúc trả về nên luôn là thông tin mới nhất
@Entity
@Table(name = "notifications", indexes = {
        // Lấy thông báo của 1 người, mới nhất trước + đếm chưa đọc
        @Index(name = "idx_noti_user_read", columnList = "user_id, is_read, id"),
        // Xoá thông báo theo lời mời khi lời mời bị từ chối / đã chấp nhận
        @Index(name = "idx_noti_type_ref", columnList = "type, reference_id")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // NGƯỜI NHẬN thông báo

    @Column(name = "actor_id", nullable = false)
    private Long actorId; // người gây ra (gửi / chấp nhận)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "reference_id")
    private Long referenceId; // id liên quan: friendRequestId...

    @Column(name = "is_read", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long userId, Long actorId, NotificationType type, Long referenceId) {
        this.userId = userId;
        this.actorId = actorId;
        this.type = type;
        this.referenceId = referenceId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getter và Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
