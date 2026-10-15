package com.example.demo.entity;

// Loại thông báo -> frontend dựa vào đây để chọn câu chữ và nút bấm
public enum NotificationType {
    FRIEND_REQUEST,  // X đã gửi lời mời kết bạn (referenceId = id lời mời)
    FRIEND_ACCEPTED  // X đã chấp nhận lời mời kết bạn của bạn
}
