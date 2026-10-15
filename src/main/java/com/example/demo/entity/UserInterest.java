package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// 1 sở thích của 1 người, mỗi sở thích 1 dòng -> sau này tìm được "người cùng sở thích"
@Entity
@Table(name = "user_interests",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_interest", columnNames = {"user_id", "name"}),
        indexes = @Index(name = "idx_interest_name", columnList = "name"))
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    public UserInterest() {
    }

    public UserInterest(Long userId, String name) {
        this.userId = userId;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
