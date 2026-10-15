package com.example.demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.dto.NotificationPageResponse;
import com.example.demo.dto.NotificationResponse;
import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import com.example.demo.util.TransactionUtils;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SocketIOServer socketServer;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
            UserRepository userRepository,
            SocketIOServer socketServer) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.socketServer = socketServer;
    }

    @Override
    @Transactional
    public void notify(Long userId, Long actorId, NotificationType type, Long referenceId) {
        if (userId.equals(actorId)) {
            return; // không tự báo cho chính mình
        }
        Notification saved = notificationRepository.save(new Notification(userId, actorId, type, referenceId));
        User actor = userRepository.findById(actorId).orElse(null);
        NotificationResponse payload = NotificationResponse.from(saved, actor);
        long unread = notificationRepository.countByUserIdAndReadFalse(userId);

        // Đợi lưu DB xong mới bắn, transaction lỗi thì không bắn
        TransactionUtils.runAfterCommit(() -> socketServer.getRoomOperations("user:" + userId)
                .sendEvent("notification", Map.of("notification", payload, "unreadCount", unread)));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long userId, int limit, Long cursor) {
        int size = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        // Lấy dư 1 để biết còn trang sau không
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Notification> list = (cursor == null)
                ? notificationRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);

        boolean hasMore = list.size() > size;
        if (hasMore) {
            list = list.subList(0, size);
        }
        Long nextCursor = hasMore ? list.get(list.size() - 1).getId() : null;

        return new NotificationPageResponse(toResponses(list), nextCursor, hasMore,
                notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại!"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    @Override
    @Transactional
    public void deleteByReference(NotificationType type, Long referenceId) {
        notificationRepository.deleteByTypeAndReferenceId(type, referenceId);
    }

    // Lấy người gây ra thông báo của cả trang bằng 1 query (tránh N+1)
    private List<NotificationResponse> toResponses(List<Notification> list) {
        Set<Long> actorIds = list.stream().map(Notification::getActorId).collect(Collectors.toSet());
        Map<Long, User> actors = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream()
                .map(n -> NotificationResponse.from(n, actors.get(n.getActorId())))
                .toList();
    }
}
