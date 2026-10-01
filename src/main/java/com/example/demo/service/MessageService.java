package com.example.demo.service;

import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.SendFileMessageRequest;

public interface MessageService {

    // 1. Gửi tin nhắn văn bản (JSON)
    MessageResponse sendMessage(Long senderId, MessageRequest request);

    // 2. Gửi ảnh / file đính kèm (MultipartForm)
    MessageResponse sendFiles(Long senderId, SendFileMessageRequest request);
}
