package com.example.demo.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.dto.BlockStatus;
import com.example.demo.dto.BlockedUserResponse;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;
import com.example.demo.entity.UserBlock;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.FriendRequestRepository;
import com.example.demo.repository.UserBlockRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BlockService;
import com.example.demo.service.NotificationService;
import com.example.demo.socket.PresenceService;
import com.example.demo.util.TransactionUtils;

@Service
public class BlockServiceImpl implements BlockService {

    private static final Logger log = LoggerFactory.getLogger(BlockServiceImpl.class);

    private final UserBlockRepository userBlockRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SocketIOServer socketServer;
    private final PresenceService presenceService;

    public BlockServiceImpl(UserBlockRepository userBlockRepository, FriendRepository friendRepository,
            FriendRequestRepository friendRequestRepository, UserRepository userRepository,
            NotificationService notificationService, SocketIOServer socketServer,
            PresenceService presenceService) {
        this.userBlockRepository = userBlockRepository;
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.socketServer = socketServer;
        this.presenceService = presenceService;
    }

    @Override
    @Transactional
    public void blockFriend(Long userId, Long friendId) {
        // 1. Kiểm tra hợp lệ
        if (userId.equals(friendId)) {
            throw new RuntimeException("Không thể chặn chính mình!");
        }
        if (!userRepository.existsById(friendId)) {
            throw new RuntimeException("Người dùng không tồn tại!");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(userId, friendId)) {
            throw new RuntimeException("Bạn đã chặn người này rồi!");
        }

        // 2. Lưu bản ghi chặn
        userBlockRepository.save(new UserBlock(userId, friendId));

        // 3. Đang là bạn bè thì huỷ kết bạn (người lạ thì bỏ qua, vẫn chặn được)
        Long minId = Math.min(userId, friendId);
        Long maxId = Math.max(userId, friendId);
        friendRepository.findByUserAIdAndUserBId(minId, maxId).ifPresent(friendRepository::delete);
        // Chặn -> 2 bên ẩn trạng thái online của nhau ngay
        TransactionUtils.runAfterCommit(() -> presenceService.onFriendshipEnded(userId, friendId));

        // 4 + 5. Xoá lời mời kết bạn đang chờ ở CẢ HAI CHIỀU,
        // kèm thông báo FRIEND_REQUEST của lời mời đó (để không bấm "Chấp nhận" vào lời mời đã mất)
        friendRequestRepository.findByFromUserIdAndToUserId(userId, friendId).ifPresent(request -> {
            notificationService.deleteByReference(NotificationType.FRIEND_REQUEST, request.getId());
            friendRequestRepository.delete(request);
        });
        friendRequestRepository.findByFromUserIdAndToUserId(friendId, userId).ifPresent(request -> {
            notificationService.deleteByReference(NotificationType.FRIEND_REQUEST, request.getId());
            friendRequestRepository.delete(request);
        });

        // 6. Sau commit: báo cho các thiết bị của CHÍNH TÔI (điện thoại, tab khác...) để cập nhật giao diện.
        // KHÔNG báo cho người bị chặn -> họ không biết mình bị chặn
        TransactionUtils.runAfterCommit(() -> socketServer.getRoomOperations("user:" + userId)
                .sendEvent("block_changed", Map.of("userId", friendId, "blocked", true)));

        log.info(">>> USER {} ĐÃ CHẶN USER {}", userId, friendId);
    }

    @Override
    @Transactional
    public void unblock(Long userId, Long targetId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(userId, targetId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa chặn người này!"));
        userBlockRepository.delete(block);

        TransactionUtils.runAfterCommit(() -> socketServer.getRoomOperations("user:" + userId)
                .sendEvent("block_changed", Map.of("userId", targetId, "blocked", false)));
        log.info(">>> USER {} ĐÃ BỎ CHẶN USER {}", userId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(Long userId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(userId);
        // Lấy thông tin người bị chặn bằng 1 query (tránh N+1)
        Set<Long> ids = blocks.stream().map(UserBlock::getBlockedId).collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return blocks.stream()
                .map(b -> BlockedUserResponse.from(b, users.get(b.getBlockedId())))
                .toList();
    }

    @Override
    public boolean isBlockedEitherWay(Long a, Long b) {
        if (a == null || b == null || a.equals(b)) {
            return false;
        }
        return userBlockRepository.existsEitherWay(a, b);
    }

    @Override
    public BlockStatus getStatus(Long me, Long other) {
        if (me == null || other == null || me.equals(other)) {
            return BlockStatus.NONE;
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(me, other)) {
            return BlockStatus.BLOCKED_BY_ME;
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(other, me)) {
            return BlockStatus.BLOCKED_ME;
        }
        return BlockStatus.NONE;
    }

    @Override
    public Set<Long> getBlockedByMeIds(Long me) {
        return new HashSet<>(userBlockRepository.findBlockedIdsByBlockerId(me));
    }

    @Override
    public Set<Long> getBlockingMeIds(Long me) {
        return new HashSet<>(userBlockRepository.findBlockerIdsByBlockedId(me));
    }

    @Override
    public Set<Long> getBlockRelatedIds(Long userId) {
        return new HashSet<>(userBlockRepository.findBlockRelatedIds(userId));
    }
}
