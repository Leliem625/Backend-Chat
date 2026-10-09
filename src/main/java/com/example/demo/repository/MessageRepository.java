package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy lịch sử tin nhắn của cuộc trò chuyện (phân trang)
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    // Lấy tất cả tin nhắn của cuộc trò chuyện theo thứ tự gửi
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    // Lấy tin nhắn theo trang, mới nhất trước:
    // - cursor = null  -> lấy tin mới nhất (lần tải đầu)
    // - cursor có giá trị -> lấy tin cũ hơn mốc thời gian đó (kéo lên xem tin cũ)
    // Số lượng tin lấy ra do Pageable quyết định (LIMIT)
    @Query("""
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND (:cursor IS NULL OR m.createdAt < :cursor)
        ORDER BY m.createdAt DESC, m.id DESC
    """)
    List<Message> findMessagesPage(@Param("conversationId") Long conversationId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);
}
