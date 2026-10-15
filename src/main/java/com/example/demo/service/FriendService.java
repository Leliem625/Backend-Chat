package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.FriendRequestResponse;
import com.example.demo.dto.FriendSearchResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.FriendRequest;

public interface FriendService {
    void sendFriendRequest(Long fromUserId, Long toUserId, String message);

    void acceptFriendRequest(Long currentUserId, Long requestId);

    void declineFriendRequest(Long currentUserId, Long requestId);

    void unfriend(Long currentUserId, Long friendId);

    List<UserResponse> getFriends(Long currentUserId);

    // Lời mời mình nhận được, kèm thông tin người gửi
    List<FriendRequestResponse> getPendingRequests(Long currentUserId);

    // Huỷ lời mời mình đã gửi (xoá luôn thông báo bên người nhận)
    void cancelFriendRequest(Long currentUserId, Long toUserId);

    boolean isFriend(Long userAId, Long userBId);

    List<FriendSearchResponse> searchFriends(Long userId, String keyword);

    // void blockFriend(Long userId, Long friendId);
}
