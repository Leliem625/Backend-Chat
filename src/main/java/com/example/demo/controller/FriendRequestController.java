package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.FriendRequestResponse;
import com.example.demo.dto.FriendSearchResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.FriendService;
import com.example.demo.util.ParseUtils;

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
        Long toUserId = ParseUtils.toLong(body.get("toUserId"));
        if (toUserId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("toUserId không được để trống!"));
        }
        String message = ParseUtils.toString(body.get("message"));
        friendService.sendFriendRequest(userId, toUserId, message);
        return ResponseEntity.ok(ApiResponse.success("Gửi lời mời kết bạn thành công!", null));
    }

    @PostMapping("/accept-request")
    public ResponseEntity<?> acceptFriendRequest(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long requestId = ParseUtils.toLong(body.get("requestId"));
        if (requestId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("requestId không được để trống!"));
        }
        friendService.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận lời mời kết bạn thành công!", null));
    }

    @PostMapping("/decline-request")
    public ResponseEntity<?> declineFriendRequest(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long requestId = ParseUtils.toLong(body.get("requestId"));
        if (requestId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("requestId không được để trống!"));
        }
        friendService.declineFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Từ chối lời mời kết bạn thành công!", null));
    }

    // POST /api/friend/cancel-request {toUserId}: huỷ lời mời mình đã gửi
    @PostMapping("/cancel-request")
    public ResponseEntity<?> cancelFriendRequest(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long toUserId = ParseUtils.toLong(body.get("toUserId"));
        if (toUserId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("toUserId không được để trống!"));
        }
        friendService.cancelFriendRequest(userId, toUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã huỷ lời mời kết bạn!", null));
    }

    @PostMapping("/unfriend")
    public ResponseEntity<?> unfriend(@RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long friendId = ParseUtils.toLong(body.get("friendId"));
        if (friendId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("friendId không được để trống!"));
        }
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
        List<FriendRequestResponse> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lời mời đang chờ thành công!", requests));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FriendSearchResponse>>> searchUsers(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công!", friendService.searchFriends(userId, q)));
    }
}
