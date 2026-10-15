package com.example.demo.service;

import com.example.demo.dto.NotificationPageResponse;
import com.example.demo.entity.NotificationType;

public interface NotificationService {
    // 1. Tạo thông báo + bắn socket "notification" cho người nhận (sau khi commit)
    void notify(Long userId, Long actorId, NotificationType type, Long referenceId);

    // 2. Danh sách thông báo của tôi, mới nhất trước (cursor = nextCursor của lần trước)
    NotificationPageResponse getNotifications(Long userId, int limit, Long cursor);

    // 3. Số thông báo chưa đọc (số trên chuông)
    long countUnread(Long userId);

    // 4. Đánh dấu 1 thông báo đã đọc (chỉ thông báo của chính mình)
    void markRead(Long userId, Long notificationId);

    // 5. Đánh dấu tất cả đã đọc
    void markAllRead(Long userId);

    // 6. Xoá thông báo gắn với 1 đối tượng đã không còn (vd lời mời kết bạn đã bị từ chối)
    void deleteByReference(NotificationType type, Long referenceId);
}
