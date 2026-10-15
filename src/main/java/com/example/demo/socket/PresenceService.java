package com.example.demo.socket;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.repository.FriendRepository;

// Trạng thái online: ai đang online (theo từng phiên / tab), và CHỈ báo cho BẠN BÈ
// (người lạ, người đã huỷ kết bạn, người bị chặn không thấy mình online)
@Component
public class PresenceService {

    // userId -> các phiên socket đang mở (1 người có thể mở nhiều tab / thiết bị)
    private final Map<Long, Set<UUID>> onlineUsers = new ConcurrentHashMap<>();
    private final SocketIOServer server;
    private final FriendRepository friendRepository;

    public PresenceService(SocketIOServer server, FriendRepository friendRepository) {
        this.server = server;
        this.friendRepository = friendRepository;
    }

    // Phiên mới kết nối: gửi danh sách BẠN BÈ đang online cho phiên đó;
    // nếu là phiên đầu tiên (vừa online) -> báo cho bạn bè
    public void handleConnect(SocketIOClient client, Long userId) {
        boolean isFirstSession;
        synchronized (onlineUsers) {
            Set<UUID> sessions = onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
            isFirstSession = sessions.isEmpty();
            sessions.add(client.getSessionId());
        }

        List<Long> friendIds = friendRepository.findFriendIdsByUserId(userId);
        Set<Long> onlineFriends = new HashSet<>();
        for (Long friendId : friendIds) {
            if (onlineUsers.containsKey(friendId)) {
                onlineFriends.add(friendId);
            }
        }
        client.sendEvent("online_users", onlineFriends);

        if (isFirstSession) {
            sendToEach(friendIds, "user_online", userId);
        }
    }

    // Phiên ngắt kết nối: tắt hết mọi phiên (thật sự offline) -> báo cho bạn bè
    public void handleDisconnect(SocketIOClient client, Long userId) {
        boolean isCompletelyOffline = false;
        synchronized (onlineUsers) {
            Set<UUID> sessions = onlineUsers.get(userId);
            if (sessions != null) {
                sessions.remove(client.getSessionId());
                if (sessions.isEmpty()) {
                    onlineUsers.remove(userId);
                    isCompletelyOffline = true;
                }
            }
        }
        if (isCompletelyOffline) {
            sendToEach(friendRepository.findFriendIdsByUserId(userId), "user_offline", userId);
        }
    }

    // Vừa thành bạn bè -> ai đang online thì người kia thấy ngay
    public void onFriendshipStarted(Long a, Long b) {
        if (isUserOnline(a)) {
            sendTo(b, "user_online", a);
        }
        if (isUserOnline(b)) {
            sendTo(a, "user_online", b);
        }
    }

    // Huỷ kết bạn / chặn -> 2 bên ẩn trạng thái online của nhau ngay (dù đang online)
    public void onFriendshipEnded(Long a, Long b) {
        sendTo(b, "user_offline", a);
        sendTo(a, "user_offline", b);
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    private void sendToEach(List<Long> receiverIds, String event, Long userId) {
        for (Long receiverId : receiverIds) {
            sendTo(receiverId, event, userId);
        }
    }

    private void sendTo(Long receiverId, String event, Long userId) {
        server.getRoomOperations("user:" + receiverId).sendEvent(event, Map.of("userId", userId));
    }
}
