package com.example.demo.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Friend;
import com.example.demo.entity.FriendRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.FriendRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FriendService;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);

    public FriendServiceImpl(FriendRepository friendRepository, 
                             FriendRequestRepository friendRequestRepository,
                             UserRepository userRepository) {
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void sendFriendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("Không thể gửi lời mời kết bạn cho chính mình!");
        }

        // 1. Kiểm tra nếu đã là bạn bè rồi thì không cho gửi nữa
        if (isFriend(fromUserId, toUserId)) {
            throw new RuntimeException("Hai người đã là bạn bè của nhau rồi!");
        }

        // 2. Kiểm tra nếu đã gửi lời mời trước đó rồi thì không cho gửi trùng
        if (friendRequestRepository.existsByFromUserIdAndToUserId(fromUserId, toUserId)) {
            throw new RuntimeException("Bạn đã gửi lời mời kết bạn cho người này rồi!");
        }

        FriendRequest friendRequest = new FriendRequest(fromUserId, toUserId, message);
        friendRequestRepository.save(friendRequest);
        log.info(">>> ĐÃ GỬI LỜI MỜI KẾT BẠN: from {} to {}", fromUserId, toUserId);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long currentUserId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findByIdAndToUserId(requestId, currentUserId)
                .orElse(null);
        if (friendRequest == null) {
            throw new RuntimeException("Lời mời không tồn tại hoặc bạn không có quyền với lời mời này!");
        }

        Long fromUserId = friendRequest.getFromUserId();

        // Tạo quan hệ bạn bè mới (@PrePersist sẽ tự đảo ID nhỏ hơn vào userAId)
        Friend friend = new Friend(fromUserId, currentUserId);
        friendRepository.save(friend);

        // Xóa lời mời kết bạn sau khi đã chấp nhận
        friendRequestRepository.delete(friendRequest);
        log.info(">>> ĐÃ CHẤP NHẬN KẾT BẠN: {} và {}", fromUserId, currentUserId);
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long currentUserId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findByIdAndToUserId(requestId, currentUserId)
                .orElse(null);
        if (friendRequest == null) {
            throw new RuntimeException("Lời mời không tồn tại hoặc bạn không có quyền với lời mời này!");
        }
        friendRequestRepository.delete(friendRequest);
        log.info(">>> ĐÃ TỪ CHỐI LỜI MỜI KẾT BẠN ID = {}", requestId);
    }

    @Override
    @Transactional
    public void unfriend(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new RuntimeException("Không thể hủy kết bạn với chính mình!");
        }

        // Chuẩn hóa: ID nhỏ là minId, ID lớn là maxId
        Long minId = Math.min(currentUserId, friendId);
        Long maxId = Math.max(currentUserId, friendId);

        Friend friend = friendRepository.findByUserAIdAndUserBId(minId, maxId).orElse(null);
        if (friend == null) {
            throw new RuntimeException("Hai người không phải bạn bè của nhau!");
        }

        friendRepository.delete(friend);
        log.info(">>> ĐÃ HỦY KẾT BẠN: {} và {}", currentUserId, friendId);
    }

    @Override
    public List<FriendRequest> getPendingRequests(Long currentUserId) {
        return friendRequestRepository.findByToUserId(currentUserId);
    }

    @Override
    public List<UserResponse> getFriends(Long currentUserId) {
        List<Long> listFriendIds = friendRepository.findFriendIdsByUserId(currentUserId);
        if (listFriendIds == null || listFriendIds.isEmpty()) {
            return List.of();
        }
        List<User> listFriends = userRepository.findAllById(listFriendIds);
        return listFriends.stream().map(UserResponse::fromUser).toList();
    }

    @Override
    public boolean isFriend(Long userAId, Long userBId) {
        if (userAId == null || userBId == null || userAId.equals(userBId)) {
            return false;
        }

        Long minId = Math.min(userAId, userBId);
        Long maxId = Math.max(userAId, userBId);

        return friendRepository.existsByUserAIdAndUserBId(minId, maxId);
    }
}
