package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.BlockStatus;
import com.example.demo.dto.BlockedUserResponse;
import com.example.demo.service.BlockService;

@RestController
@RequestMapping("/api/block")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    // POST /api/block/{userId}: chặn 1 người
    @PostMapping("/{targetId}")
    public ResponseEntity<ApiResponse<Void>> block(@RequestAttribute("userId") Long userId,
            @PathVariable Long targetId) {
        blockService.blockFriend(userId, targetId);
        return ResponseEntity.ok(ApiResponse.success("Đã chặn người dùng!", null));
    }

    // DELETE /api/block/{userId}: bỏ chặn
    @DeleteMapping("/{targetId}")
    public ResponseEntity<ApiResponse<Void>> unblock(@RequestAttribute("userId") Long userId,
            @PathVariable Long targetId) {
        blockService.unblock(userId, targetId);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ chặn người dùng!", null));
    }

    // GET /api/block: danh sách người tôi đã chặn
    @GetMapping
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers(
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chặn thành công!",
                blockService.getBlockedUsers(userId)));
    }

    // GET /api/block/{userId}/status: trạng thái chặn giữa tôi và 1 người
    @GetMapping("/{targetId}/status")
    public ResponseEntity<ApiResponse<BlockStatus>> getStatus(@RequestAttribute("userId") Long userId,
            @PathVariable Long targetId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái chặn thành công!",
                blockService.getStatus(userId, targetId)));
    }
}
