package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.SendFileMessageRequest;
import com.example.demo.service.MessageService;

@RestController
@RequestMapping("/api/message/")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send-content")
    public ResponseEntity<ApiResponse<MessageResponse>> sendContentMessage(@RequestAttribute("userId") Long userId,
            @RequestBody MessageRequest request) {
        MessageResponse message = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công!", message));
    }

    @PostMapping("/send-file")
    public ResponseEntity<ApiResponse<MessageResponse>> sendFileMessage(@RequestAttribute("userId") Long userId,
            @RequestBody SendFileMessageRequest request) {
        MessageResponse message = messageService.sendFiles(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công!", message));
    }
}
