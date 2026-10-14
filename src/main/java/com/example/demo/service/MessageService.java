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

    // 3. Lấy lịch sử tin nhắn, phân trang theo cursor là thời gian (createdAt) của
    // tin cũ nhất đang có
    MessagePageResponse getMessages(Long userId, Long conversationId, int limit, String cursor);

    // 4. Đánh dấu người dùng đã xem cuộc trò chuyện, báo qua socket cho các thành
    // viên khác
    void markSeen(Long userId, Long conversationId);

    void emitNewMessage(Long conversationId, MessageResponse message);

    // 5. Thu hồi tin nhắn (chỉ người gửi): xoá nội dung + file, báo qua socket "message_recalled"
    MessageResponse recallMessage(Long userId, Long messageId);
}
