package com.example.demo.dto;

// Quan hệ giữa người đang tìm và 1 kết quả -> frontend dựa vào đây để hiện nút
public enum FriendRelation {
    FRIEND,            // đã là bạn bè -> nút "Nhắn tin"
    REQUEST_SENT,      // mình đã gửi lời mời -> "Đã gửi lời mời"
    REQUEST_RECEIVED,  // họ đã gửi lời mời cho mình -> "Chấp nhận"
    NONE               // chưa có gì -> nút "Kết bạn"
}
