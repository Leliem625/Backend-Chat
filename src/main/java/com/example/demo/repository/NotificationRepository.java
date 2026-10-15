package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Danh sách, phân trang theo id (giống tin nhắn): lần đầu chưa có cursor
    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    // Tải tiếp: lấy thông báo cũ hơn cursor
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // Chỉ lấy thông báo của chính mình (chống đánh dấu đọc thông báo của người khác)
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId); // số trên chuông 🔔

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId);

    // Lời mời bị từ chối / đã chấp nhận -> xoá thông báo cũ để không bấm "Chấp nhận"
    // vào lời mời đã mất
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.type = :type AND n.referenceId = :referenceId")
    int deleteByTypeAndReferenceId(@Param("type") NotificationType type, @Param("referenceId") Long referenceId);
}
