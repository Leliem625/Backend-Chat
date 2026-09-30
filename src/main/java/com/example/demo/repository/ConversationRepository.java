package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // 1. Tìm cuộc trò chuyện 1-1 giữa 2 người (nếu đã từng chat với nhau)
    @Query(value = """
        SELECT c.* FROM conversations c
        JOIN conversation_participants cp1 ON c.id = cp1.conversation_id AND cp1.user_id = :userA
        JOIN conversation_participants cp2 ON c.id = cp2.conversation_id AND cp2.user_id = :userB
        WHERE c.type = 'DIRECT'
        LIMIT 1
    """, nativeQuery = true)
    Optional<Conversation> findDirectConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    // 2. Lấy danh sách tất cả cuộc trò chuyện của 1 user (sắp xếp theo tin mới nhất)
    @Query(value = """
        SELECT c.* FROM conversations c
        JOIN conversation_participants cp ON c.id = cp.conversation_id
        WHERE cp.user_id = :userId
        ORDER BY COALESCE(c.last_message_at, c.created_at) DESC
    """, nativeQuery = true)
    List<Conversation> findAllByUserId(@Param("userId") Long userId);
}
