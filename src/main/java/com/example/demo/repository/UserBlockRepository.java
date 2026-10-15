package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.UserBlock;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // 1. A có chặn B không (1 chiều) -> dùng để tính BLOCKED_BY_ME / BLOCKED_ME
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 2. A chặn B HOẶC B chặn A -> dùng khi gửi tin nhắn, gửi lời mời kết bạn, tạo chat 1-1
    @Query("""
        SELECT COUNT(b) > 0 FROM UserBlock b
        WHERE (b.blockerId = :a AND b.blockedId = :b)
           OR (b.blockerId = :b AND b.blockedId = :a)
    """)
    boolean existsEitherWay(@Param("a") Long a, @Param("b") Long b);

    // 3. Tìm bản ghi chặn để bỏ chặn
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 4. Danh sách người TÔI đã chặn, mới chặn trước (trang Cài đặt -> Người đã chặn)
    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    // 5. Mọi người có quan hệ chặn với tôi (tôi chặn họ HOẶC họ chặn tôi)
    // -> loại khỏi kết quả tìm kiếm Elasticsearch để 2 bên không thấy nhau
    @Query("""
        SELECT CASE WHEN b.blockerId = :userId THEN b.blockedId ELSE b.blockerId END
        FROM UserBlock b
        WHERE b.blockerId = :userId OR b.blockedId = :userId
    """)
    List<Long> findBlockRelatedIds(@Param("userId") Long userId);

    // 6. Id những người TÔI đã chặn
    @Query("SELECT b.blockedId FROM UserBlock b WHERE b.blockerId = :userId")
    List<Long> findBlockedIdsByBlockerId(@Param("userId") Long userId);

    // 7. Id những người ĐANG CHẶN tôi
    @Query("SELECT b.blockerId FROM UserBlock b WHERE b.blockedId = :userId")
    List<Long> findBlockerIdsByBlockedId(@Param("userId") Long userId);
}
