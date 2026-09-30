package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.ConversationResponse;
import com.example.demo.dto.CreateGroupRequest;

public interface ConversationService {

    ConversationResponse createGroupConversation(Long userId, CreateGroupRequest request);

    ConversationResponse getOrCreateDirectConversation(Long currentUserId, Long recipientId);

    ConversationResponse addMemberGroupConversation(Long conversationId, Long currentId, List<Long> memberIds);

    String deleteMemberGroupConversation(Long conversationId, Long currentId, Long memberId);

    List<ConversationResponse> getListConversation(Long userId);
}
