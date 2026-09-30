package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.UserResponse;
import com.example.demo.entity.FriendRequest;

public interface FriendService {
    void sendFriendRequest(Long fromUserId, Long toUserId, String message);

    void acceptFriendRequest(Long currentUserId, Long requestId);

    void declineFriendRequest(Long currentUserId, Long requestId);

    void unfriend(Long currentUserId, Long friendId);

    List<UserResponse> getFriends(Long currentUserId);

    List<FriendRequest> getPendingRequests(Long currentUserId);

    boolean isFriend(Long userAId, Long userBId);
}
