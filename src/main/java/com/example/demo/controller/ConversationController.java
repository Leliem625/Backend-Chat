package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ConversationResponse;
import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.service.ConversationService;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/create-group")
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroupConversation(
            @RequestAttribute("userId") Long userId, @RequestBody CreateGroupRequest request) {
        ConversationResponse conversation = conversationService.createGroupConversation(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhóm chat thành công!", conversation));

    }

    @PostMapping("/create-direct")
    public ResponseEntity<ApiResponse<ConversationResponse>> createDirectConversation(
            @RequestAttribute("userId") Long userId, Long userBid) {
        ConversationResponse conversation = conversationService.getOrCreateDirectConversation(userId, userBid);
        return ResponseEntity.ok(ApiResponse.success("Tạo cuộc hội thoại thành công!", conversation));
    }
}
