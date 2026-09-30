package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.FriendRequest;
import com.example.demo.service.FriendService;

@RestController
@RequestMapping("/api/friend")
public class FriendRequestController {
    private final FriendService friendService;

    public FriendRequestController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/send-request")
    public ResponseEntity<?> sendFriendRequets(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        String message = (String) body.get("message");
        friendService.sendFriendRequest(userId, toUserId, message);
        return ResponseEntity.ok(ApiResponse.success("Gửi lời mời kết bạn thành công!", null));
    }

    @PostMapping("/accept-request")
    public ResponseEntity<?> acceptFriendRequest(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long requestId = Long.valueOf(body.get("requestId").toString());
        friendService.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận lời mời kết bạn thành công!", null));
    }

    @PostMapping("/decline-request")
    public ResponseEntity<?> declineFriendRequest(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long requestId = Long.valueOf(body.get("requestId").toString());
        friendService.declineFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Từ chối lời mời kết bạn thành công!", null));
    }

    @PostMapping("/unfriend")
    public ResponseEntity<?> unfriend(@RequestAttribute("userId") Long userId, 
            @RequestBody Map<String, Object> body) {
        Long friendId = Long.valueOf(body.get("friendId").toString());
        friendService.unfriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("Hủy kết bạn thành công!", null));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriends(@RequestAttribute("userId") Long userId) {
        List<UserResponse> friends = friendService.getFriends(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bạn bè thành công!", friends));
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<?> getPendingRequests(@RequestAttribute("userId") Long userId) {
        List<FriendRequest> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lời mời đang chờ thành công!", requests));
    }
}
