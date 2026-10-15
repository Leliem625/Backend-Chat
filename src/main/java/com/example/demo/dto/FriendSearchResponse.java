package com.example.demo.dto;

public record FriendSearchResponse(
        Long id,
        String username,
        String avatarUrl,
        FriendRelation relation,
        Long requestId,       // REQUEST_RECEIVED: id lời mời họ gửi cho mình -> nút "Chấp nhận"; còn lại null
        String currentCity,   // nơi sống: hiển thị để phân biệt người trùng tên
        String hometown) {

}
