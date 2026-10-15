package com.example.demo.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationResponse> notifications, // mới nhất trước
    Long nextCursor,                          // id thông báo cũ nhất trang này, null nếu đã hết
    boolean hasMore,                          // còn thông báo cũ hơn để tải tiếp hay không
    long unreadCount                          // số trên chuông 🔔
) {}
