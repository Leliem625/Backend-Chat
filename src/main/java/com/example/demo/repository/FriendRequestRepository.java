package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.FriendRequest;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    // 1. Kiểm tra xem A đã gửi lời mời cho B chưa (chống gửi trùng)
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    // 2. Tìm lời mời giữa A và B (khi B bấm Chấp nhận hoặc Từ chối)
    Optional<FriendRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    Optional<FriendRequest> findByIdAndToUserId(Long id, Long toUserId);

    // 3. Lấy danh sách tất cả lời mời kết bạn MÀ TÔI NHẬN ĐƯỢC (toUserId = userId)
    List<FriendRequest> findByToUserId(Long toUserId);

    // 4. Lấy danh sách tất cả lời mời TÔI ĐÃ GỬI ĐI (fromUserId = userId)
    List<FriendRequest> findByFromUserId(Long fromUserId);
}
