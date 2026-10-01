package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.SendFileMessageRequest;
import com.example.demo.dto.UploadResponse;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.ConversationParticipant;
import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.entity.MessageType;
import com.example.demo.entity.User;
import com.example.demo.repository.ConversationParticipantRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageAttachmentRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MessageService;

@Service
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UserRepository userRepository;

    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_PREVIEW_LENGTH = 1000; // Bằng độ dài cột last_message_content
    private static final Set<String> RESOURCE_TYPES = Set.of("image", "raw");

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    public MessageServiceImpl(ConversationRepository conversationRepository, MessageRepository messageRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        Long conversationId = request.conversationId();
        String content = request.content();
        MessageType type = request.type();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Cuộc trò chuyện không tồn tại!"));
        requireParticipant(conversationId, senderId);
        Message message = new Message(conversationId, senderId, content, type);
        Message savedMessage = messageRepository.save(message);
        updateConversationAfterSend(conversation, savedMessage, savedMessage.getContent());
        return MessageResponse.fromMessage(savedMessage);
    }

    @Override
    @Transactional
    public MessageResponse sendFiles(Long senderId, SendFileMessageRequest request) {
        Long conversationId = request.conversationId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Cuộc trò chuyện không tồn tại!"));
        requireParticipant(conversationId, senderId);

        List<UploadResponse> attachments = request.attachments();
        if (attachments == null || attachments.isEmpty()) {
            throw new RuntimeException("Danh sách file đính kèm không được để trống!");
        }
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw new RuntimeException("Chỉ được gửi tối đa " + MAX_ATTACHMENTS + " file trong một tin nhắn!");
        }
        for (UploadResponse att : attachments) {
            validateAttachment(senderId, att);
        }

        // Lời nhắn kèm theo là tuỳ chọn, chuỗi rỗng coi như không có
        String content = request.content() != null ? request.content().trim() : null;
        if (content != null && content.isEmpty()) {
            content = null;
        }

        // Không tin type do client gửi: toàn ảnh -> IMAGE, có tệp khác -> FILE
        boolean allImages = attachments.stream().allMatch(att -> "image".equals(att.resourceType()));
        MessageType type = allImages ? MessageType.IMAGE : MessageType.FILE;

        Message savedMessage = messageRepository.save(new Message(conversationId, senderId, content, type));

        List<MessageAttachment> messageAttachments = new ArrayList<>();
        for (UploadResponse att : attachments) {
            messageAttachments.add(new MessageAttachment(
                    savedMessage.getId(),
                    conversationId,
                    att.url(),
                    att.publicId(),
                    att.resourceType(),
                    att.mimeType(),
                    att.name(),
                    att.size()));
        }
        List<MessageAttachment> savedAttachments = messageAttachmentRepository.saveAll(messageAttachments);

        // Preview ở danh sách cuộc trò chuyện: ưu tiên lời nhắn, không có thì hiện loại file
        String preview = content != null ? content : (allImages ? "[Hình ảnh]" : "[Tệp đính kèm]");
        updateConversationAfterSend(conversation, savedMessage, preview);

        User sender = userRepository.findById(senderId).orElse(null);
        return MessageResponse.fromMessage(savedMessage, sender, savedAttachments);
    }

    // Chỉ thành viên của cuộc trò chuyện mới được gửi tin nhắn
    private void requireParticipant(Long conversationId, Long userId) {
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new RuntimeException("Bạn không phải thành viên của cuộc trò chuyện này!");
        }
    }

    // File đính kèm phải là file do chính người gửi upload qua api /api/upload
    private void validateAttachment(Long senderId, UploadResponse att) {
        if (att == null || att.url() == null || att.publicId() == null || att.resourceType() == null) {
            throw new RuntimeException("File đính kèm không hợp lệ!");
        }
        if (!RESOURCE_TYPES.contains(att.resourceType())) {
            throw new RuntimeException("Loại file đính kèm không hợp lệ!");
        }
        if (!att.publicId().startsWith("chat/" + senderId + "/")
                || !att.url().startsWith("https://res.cloudinary.com/" + cloudName + "/")
                || !att.url().contains(att.publicId())) {
            throw new RuntimeException("File đính kèm không hợp lệ!");
        }
    }

    // Cập nhật preview tin nhắn cuối của cuộc trò chuyện và số tin chưa đọc của các thành viên
    private void updateConversationAfterSend(Conversation conversation, Message savedMessage, String preview) {
        Long senderId = savedMessage.getSenderId();

        if (preview != null && preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH);
        }
        conversation.setLastMessageId(savedMessage.getId().toString());
        conversation.setLastMessageContent(preview);
        conversation.setLastMessageSenderId(senderId);
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversationRepository.save(conversation);

        List<ConversationParticipant> conversationPar = conversationParticipantRepository
                .findByConversationId(conversation.getId());
        for (ConversationParticipant participant : conversationPar) {
            // Chỉ tăng unreadCount cho những người nhận (khác senderId)
            if (!participant.getUserId().equals(senderId)) {
                int currentUnread = participant.getUnreadCount() != null ? participant.getUnreadCount() : 0;
                participant.setUnreadCount(currentUnread + 1);
            } else {
                // Đối với người gửi: số tin chưa đọc luôn là 0 và vừa xem tin nhắn
                participant.setUnreadCount(0);
                participant.setLastSeenAt(LocalDateTime.now());
            }
        }
        conversationParticipantRepository.saveAll(conversationPar);
    }
}
