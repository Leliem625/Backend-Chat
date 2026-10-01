package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.MessageAttachment;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    // Lấy tất cả file đính kèm thuộc về 1 tin nhắn
    List<MessageAttachment> findByMessageId(Long messageId);

    // Lấy thư viện ảnh/file của cuộc trò chuyện (lọc theo loại: image, video, raw)
    List<MessageAttachment> findByConversationIdAndResourceTypeOrderByIdDesc(Long conversationId, String resourceType);
}
