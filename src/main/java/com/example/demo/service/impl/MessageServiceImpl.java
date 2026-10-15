package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.dto.MessagePageResponse;
import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.SeenInfo;
import com.example.demo.dto.SendFileMessageRequest;
import com.example.demo.dto.UploadResponse;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.ConversationParticipant;
import com.example.demo.entity.ConversationType;
import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.entity.MessageType;
import com.example.demo.entity.User;
import com.example.demo.repository.ConversationParticipantRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageAttachmentRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BlockService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.MessageService;
import com.example.demo.util.TransactionUtils;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UserRepository userRepository;
    private final SocketIOServer socketServer;
    private final FileStorageService fileStorageService;
    private final BlockService blockService;

    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PREVIEW_LENGTH = 1000; // Bằng độ dài cột last_message_content
    private static final Set<String> RESOURCE_TYPES = Set.of("image", "raw");
    private static final String RECALLED_PREVIEW = "Tin nhắn đã được thu hồi";

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    public MessageServiceImpl(ConversationRepository conversationRepository, MessageRepository messageRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            UserRepository userRepository,
            SocketIOServer socketServer,
            FileStorageService fileStorageService,
            BlockService blockService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.userRepository = userRepository;
        this.socketServer = socketServer;
        this.fileStorageService = fileStorageService;
        this.blockService = blockService;
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
        requireNotBlocked(conversation, senderId);
        Message message = new Message(conversationId, senderId, content, type);
        Message savedMessage = messageRepository.save(message);
        updateConversationAfterSend(conversation, savedMessage, savedMessage.getContent());
        // Kèm người gửi để bên nhận có avatar / tên (giống sendFiles)
        User sender = userRepository.findById(senderId).orElse(null);
        MessageResponse response = MessageResponse.fromMessage(savedMessage, sender, null);
        emitNewMessage(conversationId, response);
        return response;
    }

    @Override
    @Transactional
    public MessageResponse sendFiles(Long senderId, SendFileMessageRequest request) {
        Long conversationId = request.conversationId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Cuộc trò chuyện không tồn tại!"));
        requireParticipant(conversationId, senderId);
        requireNotBlocked(conversation, senderId);

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

        // Preview ở danh sách cuộc trò chuyện: ưu tiên lời nhắn, không có thì hiện loại
        // file
        String preview = content != null ? content : (allImages ? "[Hình ảnh]" : "[Tệp đính kèm]");
        updateConversationAfterSend(conversation, savedMessage, preview);

        User sender = userRepository.findById(senderId).orElse(null);
        emitNewMessage(conversationId, MessageResponse.fromMessage(savedMessage, sender, savedAttachments));
        return MessageResponse.fromMessage(savedMessage, sender, savedAttachments);
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(Long userId, Long conversationId, int limit, String cursor) {
        requireParticipant(conversationId, userId);

        // Chặn limit trong khoảng 1..MAX_PAGE_SIZE để client không lấy quá nhiều 1 lần
        int size = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        // Lấy dư 1 tin: nếu có tin thứ size+1 nghĩa là vẫn còn trang cũ hơn
        Pageable pageable = PageRequest.of(0, size + 1);

        // Chưa có cursor -> null -> query lấy tin mới nhất; có cursor -> lấy tin cũ hơn
        // mốc đó
        LocalDateTime cursorTime = (cursor == null || cursor.isBlank()) ? null : parseCursor(cursor);
        List<Message> messages = messageRepository.findMessagesPage(conversationId, cursorTime, pageable);

        boolean hasMore = messages.size() > size;
        if (hasMore) {
            messages = messages.subList(0, size); // Bỏ tin dư, chỉ dùng để kiểm tra
        }

        // nextCursor = createdAt của tin cũ nhất trong trang (tin cuối danh sách vì
        // đang sắp mới -> cũ)
        LocalDateTime nextCursor = hasMore ? messages.get(messages.size() - 1).getCreatedAt() : null;

        // Lấy file đính kèm và người gửi của cả trang bằng 2 query, rồi ghép theo từng
        // tin nhắn
        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        Map<Long, List<MessageAttachment>> attachmentsByMessage = messageIds.isEmpty()
                ? Map.of()
                : messageAttachmentRepository.findByMessageIdIn(messageIds).stream()
                        .collect(Collectors.groupingBy(MessageAttachment::getMessageId));

        Set<Long> senderIds = messages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> sendersById = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Trả về theo thứ tự mới nhất trước, client tự đảo lại khi hiển thị
        List<MessageResponse> items = messages.stream()
                .map(m -> MessageResponse.fromMessage(
                        m,
                        sendersById.get(m.getSenderId()),
                        attachmentsByMessage.get(m.getId())))
                .toList();
        return new MessagePageResponse(items, nextCursor, hasMore, getSeenBy(conversationId, userId));
    }

    @Override
    @Transactional
    public void markSeen(Long userId, Long conversationId) {
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của cuộc trò chuyện này!"));
        participant.setUnreadCount(0);
        participant.setLastSeenAt(LocalDateTime.now());
        conversationParticipantRepository.save(participant);

        // Báo cho các thành viên khác để họ cập nhật avatar "đã xem" ngay
        User user = userRepository.findById(userId).orElse(null);
        SeenInfo seen = new SeenInfo(userId, user != null ? user.getAvatarUrl() : null, participant.getLastSeenAt());
        Map<String, Object> payload = Map.of("conversationId", conversationId, "seen", seen);
        for (Long memberId : conversationParticipantRepository.findUserIdsByConversationId(conversationId)) {
            // Không báo "đã xem" cho người có quan hệ chặn với mình
            if (!memberId.equals(userId) && !blockService.isBlockedEitherWay(userId, memberId)) {
                socketServer.getRoomOperations("user:" + memberId).sendEvent("message_seen", payload);
            }
        }

        // Báo cho CHÍNH MÌNH (mọi thiết bị / tab đang mở) để xoá badge chưa đọc của cuộc trò chuyện này
        TransactionUtils.runAfterCommit(() -> socketServer.getRoomOperations("user:" + userId)
                .sendEvent("conversation_read", Map.of("conversationId", conversationId)));
    }

    // Thời điểm xem lần cuối của các thành viên khác (trừ người đang hỏi)
    private List<SeenInfo> getSeenBy(Long conversationId, Long currentUserId) {
        List<ConversationParticipant> others = conversationParticipantRepository.findByConversationId(conversationId)
                .stream()
                .filter(p -> !p.getUserId().equals(currentUserId) && p.getLastSeenAt() != null)
                .toList();
        if (others.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = userRepository
                .findAllById(others.stream().map(ConversationParticipant::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return others.stream()
                .map(p -> {
                    User u = usersById.get(p.getUserId());
                    return new SeenInfo(p.getUserId(), u != null ? u.getAvatarUrl() : null, p.getLastSeenAt());
                })
                .toList();
    }

    private LocalDateTime parseCursor(String cursor) {
        try {
            return LocalDateTime.parse(cursor);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(cursor)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw new RuntimeException("Cursor không hợp lệ, cần dạng ngày giờ ISO (vd: 2026-10-08T10:00:00)!");
            }
        }
    }

    // Chat 1-1: một trong hai người đã chặn người kia thì không gửi được.
    // Nhóm vẫn gửi bình thường (giống Messenger). Câu báo lỗi chung chung để không lộ ai chặn ai
    private void requireNotBlocked(Conversation conversation, Long senderId) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return;
        }
        for (Long memberId : conversationParticipantRepository.findUserIdsByConversationId(conversation.getId())) {
            if (!memberId.equals(senderId) && blockService.isBlockedEitherWay(senderId, memberId)) {
                throw new RuntimeException("Không thể gửi tin nhắn cho người này!");
            }
        }
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

    // Cập nhật preview tin nhắn cuối của cuộc trò chuyện và số tin chưa đọc của các
    // thành viên
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

    @Override
    public void emitNewMessage(Long conversationId, MessageResponse message) {
        List<Long> memberIds = conversationParticipantRepository.findUserIdsByConversationId(conversationId);
        TransactionUtils.runAfterCommit(() -> {
            for (Long memberId : memberIds) {
                socketServer.getRoomOperations("user:" + memberId).sendEvent("new_message", message);
            }
        });
    }

    @Override
    @Transactional
    public MessageResponse recallMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại!"));
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Bạn chỉ có thể thu hồi tin nhắn của chính mình!");
        }
        if (message.isRecalled()) {
            throw new RuntimeException("Tin nhắn đã được thu hồi trước đó!");
        }
        Long conversationId = message.getConversationId();
        // Người đã rời nhóm thì không còn quyền thao tác trong cuộc trò chuyện
        requireParticipant(conversationId, userId);

        // Xoá nội dung, đánh dấu đã thu hồi
        message.setContent(null);
        message.setRecalled(true);
        message.setRecalledAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        // Xoá file đính kèm trong DB; file trên Cloudinary xoá sau khi commit
        // (lỗi Cloudinary chỉ ghi log, không làm hỏng việc thu hồi)
        List<MessageAttachment> attachments = messageAttachmentRepository.findByMessageId(messageId);
        if (!attachments.isEmpty()) {
            messageAttachmentRepository.deleteAll(attachments);
            TransactionUtils.runAfterCommit(() -> {
                for (MessageAttachment att : attachments) {
                    try {
                        fileStorageService.delete(att.getPublicId(), att.getResourceType());
                    } catch (Exception e) {
                        log.warn("Không xoá được file {} trên Cloudinary: {}", att.getPublicId(), e.getMessage());
                    }
                }
            });
        }

        // Nếu đây là tin cuối cùng -> đổi preview ở danh sách cuộc trò chuyện
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (messageId.toString().equals(conversation.getLastMessageId())) {
                conversation.setLastMessageContent(RECALLED_PREVIEW);
                conversationRepository.save(conversation);
            }
        });

        User sender = userRepository.findById(userId).orElse(null);
        MessageResponse response = MessageResponse.fromMessage(saved, sender, null);

        // Báo cho mọi thành viên (kể cả các thiết bị khác của người thu hồi)
        List<Long> memberIds = conversationParticipantRepository.findUserIdsByConversationId(conversationId);
        TransactionUtils.runAfterCommit(() -> {
            for (Long memberId : memberIds) {
                socketServer.getRoomOperations("user:" + memberId).sendEvent("message_recalled", response);
            }
        });
        return response;
    }
}
