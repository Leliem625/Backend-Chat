package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Quan hệ chặn có CHIỀU: A chặn B khác B chặn A (khác bảng friends không phân biệt chiều)
@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = {
        // Mỗi cặp (người chặn, người bị chặn) chỉ 1 dòng -> chống chặn trùng
        // + đóng vai trò index để tra "A có chặn B không" thật nhanh
        @UniqueConstraint(name = "uk_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
    },
    indexes = {
        // Tra chiều ngược lại: "ai đang chặn tôi"
        @Index(name = "idx_block_blocked", columnList = "blocked_id")
    }
)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người bấm "Chặn"
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    // Người bị chặn
    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UserBlock() {
    }

    public UserBlock(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getter và Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBlockerId() {
        return blockerId;
    }

    public void setBlockerId(Long blockerId) {
        this.blockerId = blockerId;
    }

    public Long getBlockedId() {
        return blockedId;
    }

    public void setBlockedId(Long blockedId) {
        this.blockedId = blockedId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserBlock{" +
                "id=" + id +
                ", blockerId=" + blockerId +
                ", blockedId=" + blockedId +
                ", createdAt=" + createdAt +
                '}';
    }
}
