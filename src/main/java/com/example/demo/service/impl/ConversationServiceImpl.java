package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ConversationResponse;
import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.ConversationParticipant;
import com.example.demo.entity.ConversationType;
import com.example.demo.entity.ParticipantRole;
import com.example.demo.entity.User;
import com.example.demo.repository.ConversationParticipantRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ConversationService;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserRepository userRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   ConversationParticipantRepository conversationParticipantRepository,
                                   UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ConversationResponse createGroupConversation(Long userId, CreateGroupRequest request) {
        Conversation conversation = new Conversation(
                ConversationType.GROUP,
                request.name(),
                userId);
        Conversation savedConversation = conversationRepository.save(conversation);

        // 1. Gom danh sách thành viên được mời (loại bỏ ID của người tạo nếu gửi trùng)
        Set<Long> otherMemberIds = new HashSet<>(request.memberIds());
        otherMemberIds.remove(userId);

        List<ConversationParticipant> participants = new ArrayList<>();

        // 2. Người tạo nhóm được gán quyền Quản trị viên (ADMIN)
        participants.add(new ConversationParticipant(savedConversation.getId(), userId, ParticipantRole.ADMIN));

        // 3. Các thành viên được mời là thành viên thường (MEMBER)
        for (Long memberId : otherMemberIds) {
            participants.add(new ConversationParticipant(savedConversation.getId(), memberId, ParticipantRole.MEMBER));
        }

        conversationParticipantRepository.saveAll(participants);

        return new ConversationResponse(
                savedConversation.getId(),
                savedConversation.getType(),
                savedConversation.getConversationName(),
                null,
                null,
                null,
                savedConversation.getCreatedAt(),
                0,
                savedConversation.getCreatedBy());
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateDirectConversation(Long currentUserId, Long recipientId) {
        // 1. Validate: Không tự chat với chính mình
        if (currentUserId.equals(recipientId)) {
            throw new RuntimeException("Không thể tạo cuộc trò chuyện 1-1 với chính mình!");
        }

        // 2. Validate: Người nhận phải tồn tại trong hệ thống
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Người dùng nhận tin nhắn không tồn tại!"));

        // 3. Kiểm tra xem đã từng có cuộc trò chuyện 1-1 giữa 2 người chưa
        Optional<Conversation> existing = conversationRepository.findDirectConversation(currentUserId, recipientId);
        if (existing.isPresent()) {
            Conversation conv = existing.get();

            // Lấy số tin chưa đọc của chính mình trong cuộc trò chuyện này
            Integer unreadCount = conversationParticipantRepository
                    .findByConversationIdAndUserId(conv.getId(), currentUserId)
                    .map(ConversationParticipant::getUnreadCount)
                    .orElse(0);

            return new ConversationResponse(
                    conv.getId(),
                    conv.getType(),
                    recipient.getUsername(),
                    recipient.getAvatarUrl(),
                    conv.getLastMessageContent(),
                    conv.getLastMessageSenderId(),
                    conv.getLastMessageAt() != null ? conv.getLastMessageAt() : conv.getCreatedAt(),
                    unreadCount,
                    conv.getCreatedBy()
            );
        }

        // 4. Nếu chưa có -> Tạo mới cuộc trò chuyện 1-1
        Conversation conversation = new Conversation(ConversationType.DIRECT, null, currentUserId);
        Conversation savedConversation = conversationRepository.save(conversation);

        // 5. Thêm cả 2 người vào bảng conversation_participants
        List<ConversationParticipant> participants = List.of(
                new ConversationParticipant(savedConversation.getId(), currentUserId),
                new ConversationParticipant(savedConversation.getId(), recipientId)
        );
        conversationParticipantRepository.saveAll(participants);

        // 6. Trả về Response hiển thị tên và avatar của đối phương
        return new ConversationResponse(
                savedConversation.getId(),
                savedConversation.getType(),
                recipient.getUsername(),
                recipient.getAvatarUrl(),
                null,
                null,
                savedConversation.getCreatedAt(),
                0,
                savedConversation.getCreatedBy()
        );
    }

    @Override
    @Transactional
    public ConversationResponse addMemberGroupConversation(Long conversationId, Long currentId, List<Long> memberIds) {
        // 1. Kiểm tra cuộc trò chuyện có tồn tại và phải là GROUP không
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Cuộc trò chuyện không tồn tại!"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Chỉ có thể thêm thành viên vào nhóm chat!");
        }

        // 2. Kiểm tra xem người thực hiện (currentId) có phải là ADMIN của nhóm không
        ConversationParticipant currentParticipant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversationId, currentId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này!"));

        if (currentParticipant.getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Chỉ quản trị viên mới có quyền thêm thành viên vào nhóm!");
        }

        // 3. Lấy danh sách ID các thành viên ĐÃ CÓ trong nhóm để tránh thêm trùng
        List<Long> existingMemberIds = conversationParticipantRepository.findUserIdsByConversationId(conversationId);

        // 4. Lọc ra các thành viên mới chưa có trong nhóm
        List<ConversationParticipant> newParticipants = memberIds.stream()
                .filter(id -> !existingMemberIds.contains(id))
                .distinct()
                .map(id -> new ConversationParticipant(conversationId, id, ParticipantRole.MEMBER))
                .toList();

        if (!newParticipants.isEmpty()) {
            conversationParticipantRepository.saveAll(newParticipants);
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getConversationName(),
                null,
                conversation.getLastMessageContent(),
                conversation.getLastMessageSenderId(),
                conversation.getLastMessageAt() != null ? conversation.getLastMessageAt() : conversation.getCreatedAt(),
                currentParticipant.getUnreadCount(),
                conversation.getCreatedBy()
        );
    }
}
