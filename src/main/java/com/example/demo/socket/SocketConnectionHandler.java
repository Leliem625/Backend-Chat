package com.example.demo.socket;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.repository.ConversationParticipantRepository;

@Component
public class SocketConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);
    private final Map<Long, Set<UUID>> onlineUsers = new ConcurrentHashMap<>();
    private final ConversationParticipantRepository participantRepository;

    public SocketConnectionHandler(SocketIOServer server, ConversationParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
        server.addConnectListener(client -> {
            Long userId = client.get("userId");
            if (userId == null) {
                return;
            }

            client.joinRoom("user:" + userId);
            UUID sessionId = client.getSessionId();

            boolean isFirstSession = false;
            synchronized (onlineUsers) {
                Set<UUID> sessions = onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
                if (sessions.isEmpty()) {
                    isFirstSession = true;
                }
                sessions.add(sessionId);
            }

            log.info("User {} đã kết nối (Session: {})", userId, sessionId);

            // Gửi danh sách online cho chính người vừa kết nối
            Set<Long> currentOnlineIds = Collections.unmodifiableSet(onlineUsers.keySet());
            client.sendEvent("online_users", currentOnlineIds);

            // Nếu đây là phiên kết nối đầu tiên của user -> Báo cho tất cả người khác biết
            // user này vừa Online
            if (isFirstSession) {
                server.getBroadcastOperations().sendEvent("user_online", Map.of("userId", userId));
            }
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
                if (!memberId.equals(userId)) {
                    server.getRoomOperations("user:" + memberId).sendEvent("typing", payload);
                }
            }
        });
        server.addDisconnectListener(client -> {
            Long userId = client.get("userId");
            if (userId == null) {
                return;
            }

            UUID sessionId = client.getSessionId();
            boolean isCompletelyOffline = false;

            synchronized (onlineUsers) {
                Set<UUID> sessions = onlineUsers.get(userId);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        onlineUsers.remove(userId);
                        isCompletelyOffline = true;
                    }
                }
            }

            log.info("User {} đã ngắt kết nối (Session: {}) - isCompletelyOffline: {}", userId, sessionId,
                    isCompletelyOffline);

            // Nếu user đã tắt hết mọi phiên kết nối -> Báo cho tất cả người khác biết user
            // này đã Offline!
            if (isCompletelyOffline) {
                server.getBroadcastOperations().sendEvent("user_offline", Map.of("userId", userId));
            }
        });
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    public Set<Long> getOnlineUserIds() {
        return Collections.unmodifiableSet(onlineUsers.keySet());
    }
}