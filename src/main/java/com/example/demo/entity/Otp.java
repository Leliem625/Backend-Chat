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

@Entity
@Table(name = "otps", indexes = {
        // Đánh index cho email để tìm kiếm mã OTP nhanh nhất
        @Index(name = "idx_otp_email", columnList = "email")
})
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 10)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Đánh dấu mã đã sử dụng chưa (chống dùng lại OTP cũ)
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Otp() {
    }

    public Otp(String email, String otp, LocalDateTime expiresAt) {
        this.email = email;
        this.otp = otp;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isUsed == null) {
            this.isUsed = false;
        }
    }

    // --- Getter và Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Otp{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", otp='" + otp + '\'' +
                ", expiresAt=" + expiresAt +
                ", isUsed=" + isUsed +
                ", createdAt=" + createdAt +
                '}';
    }
}
