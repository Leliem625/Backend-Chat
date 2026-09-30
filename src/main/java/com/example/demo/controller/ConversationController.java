package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ConversationResponse;
import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.service.ConversationService;

import jakarta.validation.Valid;

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
    };

    @PostMapping("/add-member")
    public ResponseEntity<ApiResponse<ConversationResponse>> addMemberGroupConversation(
            @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") Long userId) {
        Long conversationId = (Long) body.get("conversationId");
        List<?> rawList = (List<?>) body.get("memberIds");
        List<Long> memberIds = rawList.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

        ConversationResponse conversation = conversationService.addMemberGroupConversation(conversationId, userId,
                memberIds);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công!", conversation));
    }

    @PostMapping("/delete-member")
    public ResponseEntity<?> deleteMemberGroupConversation(@Valid @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") Long userId) {

        Long conversationId = (Long) body.get("conversationId");
        Long memberId = (Long) body.get("memberId");
        if (conversationId == null || memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("conversationId và memberId không được để trống!"));
        }
        ;
        String nameMemberDeleted = conversationService.deleteMemberGroupConversation(conversationId, userId, memberId);
        return ResponseEntity.ok(ApiResponse.success("Xoá thành công thành viên", nameMemberDeleted));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getListConversation(
            @RequestAttribute("userId") Long userId) {
        List<ConversationResponse> listConversations = conversationService.getListConversation(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cuộc trò chuyện thành công!", listConversations));
    }
}
