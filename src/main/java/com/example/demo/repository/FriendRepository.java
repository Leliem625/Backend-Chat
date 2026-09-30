package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Friend;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // 1. Kiểm tra 2 người đã là bạn bè chưa
    boolean existsByUserAIdAndUserBId(Long userAId, Long userBId);

    // 2. Tìm bản ghi kết bạn giữa 2 người
    Optional<Friend> findByUserAIdAndUserBId(Long userAId, Long userBId);

    // 3. Lấy tất cả quan hệ bạn bè của 1 user
    List<Friend> findByUserAIdOrUserBId(Long userAId, Long userBId);

    @Query(value = """
                SELECT user_b_id FROM friends WHERE user_a_id = :userId
                UNION
                SELECT user_a_id FROM friends WHERE user_b_id = :userId
            """, nativeQuery = true)
    List<Long> findFriendIdsByUserId(@Param("userId") Long userId);
}
