package com.example.demo.service;

import java.util.List;
import java.util.Set;

import com.example.demo.dto.BlockStatus;
import com.example.demo.dto.BlockedUserResponse;

public interface BlockService {
    // 1. Chặn: huỷ kết bạn, xoá lời mời + thông báo liên quan, báo socket cho các thiết bị của tôi
    void blockFriend(Long userId, Long friendId);

    // 2. Bỏ chặn (KHÔNG khôi phục quan hệ bạn bè)
    void unblock(Long userId, Long targetId);

    // 3. Danh sách người tôi đã chặn (trang Cài đặt -> Người đã chặn)
    List<BlockedUserResponse> getBlockedUsers(Long userId);

    // 4. A chặn B HOẶC B chặn A -> không cho nhắn tin / gửi lời mời / tạo chat 1-1
    boolean isBlockedEitherWay(Long a, Long b);

    // 5. Trạng thái chặn giữa tôi và 1 người (cho frontend khoá ô nhập tin nhắn)
    BlockStatus getStatus(Long me, Long other);

    // 6. Id những người tôi đã chặn / đang chặn tôi (tính blockStatus cho cả danh sách bằng 2 query)
    Set<Long> getBlockedByMeIds(Long me);

    Set<Long> getBlockingMeIds(Long me);

    // 7. Mọi người có quan hệ chặn với tôi (2 chiều) -> loại khỏi kết quả tìm kiếm
    Set<Long> getBlockRelatedIds(Long userId);
}
