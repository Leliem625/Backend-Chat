package com.example.demo.dto;

// Trạng thái chặn giữa tôi và 1 người khác -> frontend dựa vào đây để khoá ô nhập tin nhắn
public enum BlockStatus {
    NONE,           // không ai chặn ai -> bình thường
    BLOCKED_BY_ME,  // tôi đã chặn họ -> "Bạn đã chặn X" + nút [Bỏ chặn]
    BLOCKED_ME      // họ đã chặn tôi -> "Bạn không thể trả lời cuộc trò chuyện này" (không nói rõ là bị chặn)
}
