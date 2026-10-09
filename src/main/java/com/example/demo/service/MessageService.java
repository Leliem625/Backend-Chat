package com.example.demo.service;

import com.example.demo.dto.MessagePageResponse;
import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.SendFileMessageRequest;

public interface MessageService {

    // 1. Gửi tin nhắn văn bản (JSON)
    MessageResponse sendMessage(Long senderId, MessageRequest request);

    // 2. Gửi ảnh / file đính kèm (MultipartForm)
    MessageResponse sendFiles(Long senderId, SendFileMessageRequest request);

    // 3. Lấy lịch sử tin nhắn, phân trang theo cursor là thời gian (createdAt) của tin cũ nhất đang có
    MessagePageResponse getMessages(Long userId, Long conversationId, int limit, String cursor);

    // 4. Đánh dấu người dùng đã xem cuộc trò chuyện, báo qua socket cho các thành viên khác
    void markSeen(Long userId, Long conversationId);
}
