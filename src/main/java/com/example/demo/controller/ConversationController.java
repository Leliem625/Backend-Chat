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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ConversationResponse;
import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.service.ConversationService;
import com.example.demo.util.ParseUtils;

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
            @RequestAttribute("userId") Long userId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "userBid", required = false) Long queryUserBid) {
        Long userBid = queryUserBid != null ? queryUserBid : (body != null ? ParseUtils.toLong(body.get("userBid")) : null);
        if (userBid == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("userBid không được để trống!"));
        }
        ConversationResponse conversation = conversationService.getOrCreateDirectConversation(userId, userBid);
        return ResponseEntity.ok(ApiResponse.success("Tạo cuộc hội thoại thành công!", conversation));
    }

    @PostMapping("/add-member")
    public ResponseEntity<ApiResponse<ConversationResponse>> addMemberGroupConversation(
            @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") Long userId) {
        Long conversationId = ParseUtils.toLong(body.get("conversationId"));
        if (conversationId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("conversationId không được để trống!"));
        }

        List<Long> memberIds = ParseUtils.toLongList(body.get("memberIds"));
        if (memberIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("memberIds không được để trống!"));
        }

        ConversationResponse conversation = conversationService.addMemberGroupConversation(conversationId, userId,
                memberIds);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công!", conversation));
    }

    @PostMapping("/delete-member")
    public ResponseEntity<?> deleteMemberGroupConversation(@Valid @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") Long userId) {

        Long conversationId = ParseUtils.toLong(body.get("conversationId"));
        Long memberId = ParseUtils.toLong(body.get("memberId"));
        if (conversationId == null || memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("conversationId và memberId không được để trống!"));
        }

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
