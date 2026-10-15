package com.example.demo.socket;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.repository.ConversationParticipantRepository;
import com.example.demo.service.BlockService;

@Component
public class SocketConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);
    private final ConversationParticipantRepository participantRepository;
    private final BlockService blockService;
    private final PresenceService presenceService;

    public SocketConnectionHandler(SocketIOServer server, ConversationParticipantRepository participantRepository,
            BlockService blockService, PresenceService presenceService) {
        this.participantRepository = participantRepository;
        this.blockService = blockService;
        this.presenceService = presenceService;
        server.addConnectListener(client -> {
            Long userId = client.get("userId");
            if (userId == null) {
                return;
            }

            client.joinRoom("user:" + userId);
            log.info("User {} đã kết nối (Session: {})", userId, client.getSessionId());
            // Trạng thái online chỉ gửi cho / nhận từ bạn bè
            presenceService.handleConnect(client, userId);
        });
        server.addEventListener("typing", Map.class, (client, data, ack) -> {
            Long userId = client.get("userId");
            Long conversationId = Long.valueOf(data.get("conversationId").toString());
            boolean isTyping = Boolean.TRUE.equals(data.get("isTyping"));
            // Chỉ thành viên của cuộc trò chuyện mới được báo
            if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId))
                return;
            Map<String, Object> payload = Map.of("conversationId", conversationId, "userId", userId, "isTyping",
                    isTyping);
            for (Long memberId : participantRepository.findUserIdsByConversationId(conversationId)) {
                // Không báo "đang soạn" cho người có quan hệ chặn với mình
                if (!memberId.equals(userId) && !blockService.isBlockedEitherWay(userId, memberId)) {
                    server.getRoomOperations("user:" + memberId).sendEvent("typing", payload);
                }
            }
        });
        server.addDisconnectListener(client -> {
            Long userId = client.get("userId");
            if (userId == null) {
                return;
            }

            log.info("User {} đã ngắt kết nối (Session: {})", userId, client.getSessionId());
            presenceService.handleDisconnect(client, userId);
        });
    }
}
