package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ConversationParticipant;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    // 1. Kiểm tra xem user có nằm trong cuộc trò chuyện không (kiểm tra quyền truy
    // cập)
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    // 2. Tìm bản ghi thành viên cụ thể (để reset unreadCount = 0 hoặc cập nhật
    // lastSeenAt)
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    // 3. Lấy danh sách tất cả thành viên của 1 cuộc trò chuyện
    List<ConversationParticipant> findByConversationId(Long conversationId);

    // 4. Lấy nhanh danh sách ID của tất cả thành viên (để bắn tin nhắn qua
    // WebSocket tới họ)
    @Query("SELECT cp.userId FROM ConversationParticipant cp WHERE cp.conversationId = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") Long conversationId);

    // 5. Đếm số lượng thành viên trong nhóm
    long countByConversationId(Long conversationId);

    // 6. Xóa thành viên khỏi nhóm (khi rời nhóm hoặc bị xóa)
    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
