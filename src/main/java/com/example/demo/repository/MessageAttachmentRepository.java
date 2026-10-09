package com.example.demo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.MessageAttachment;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    // Lấy tất cả file đính kèm thuộc về 1 tin nhắn
    List<MessageAttachment> findByMessageId(Long messageId);

    // Lấy file đính kèm của nhiều tin nhắn cùng lúc (1 query cho cả trang tin nhắn, tránh N+1)
    List<MessageAttachment> findByMessageIdIn(Collection<Long> messageIds);

    // Lấy thư viện ảnh/file của cuộc trò chuyện (lọc theo loại: image, video, raw)
    List<MessageAttachment> findByConversationIdAndResourceTypeOrderByIdDesc(Long conversationId, String resourceType);
}
