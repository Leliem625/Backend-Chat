package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;

public record NotificationResponse(
    Long id,
    NotificationType type,      // FRIEND_REQUEST / FRIEND_ACCEPTED -> frontend chọn câu chữ + nút
    Actor actor,                // người gây ra thông báo (người gửi / người chấp nhận)
    Long referenceId,           // FRIEND_REQUEST: id lời mời -> dùng cho nút "Chấp nhận" / "Từ chối"
    boolean read,               // đã đọc chưa
    LocalDateTime createdAt
) {
    // Chỉ thông tin công khai của người gây ra thông báo (KHÔNG trả email, phone)
    public record Actor(Long id, String username, String avatarUrl) {
        public static Actor fromUser(User user) {
            return user == null ? null : new Actor(user.getId(), user.getUsername(), user.getAvatarUrl());
        }
    }

    public static NotificationResponse from(Notification n, User actor) {
        return new NotificationResponse(
            n.getId(),
            n.getType(),
            Actor.fromUser(actor),
            n.getReferenceId(),
            n.isRead(),
            n.getCreatedAt()
        );
    }
}
