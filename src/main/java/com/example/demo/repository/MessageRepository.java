package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy lịch sử tin nhắn của cuộc trò chuyện (phân trang)
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    // Lấy tất cả tin nhắn của cuộc trò chuyện theo thứ tự gửi
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);
}
