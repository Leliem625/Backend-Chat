package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.NotificationPageResponse;
import com.example.demo.service.NotificationService;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // GET /api/notification?limit=20&cursor=31
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long cursor) {
        NotificationPageResponse page = notificationService.getNotifications(userId, limit, cursor);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông báo thành công!", page));
    }

    // GET /api/notification/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread(@RequestAttribute("userId") Long userId) {
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số thông báo chưa đọc thành công!", Map.of("count", count)));
    }

    // POST /api/notification/{id}/read
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc!", null));
    }

    // POST /api/notification/read-all
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả đã đọc!", null));
    }
}
