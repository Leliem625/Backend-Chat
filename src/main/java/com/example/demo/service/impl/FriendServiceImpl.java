package com.example.demo.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.FriendRelation;
import com.example.demo.dto.FriendSearchResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Friend;
import com.example.demo.entity.FriendRequest;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.FriendRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.search.UserDocument;
import com.example.demo.service.BlockService;
import com.example.demo.service.FriendService;
import com.example.demo.service.NotificationService;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);
    private final ElasticsearchOperations elasticsearchOperations;
    private final NotificationService notificationService;
    private final BlockService blockService;

    public FriendServiceImpl(FriendRepository friendRepository,
            FriendRequestRepository friendRequestRepository,
            UserRepository userRepository,
            ElasticsearchOperations elasticsearchOperations,
            NotificationService notificationService,
            BlockService blockService) {
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.notificationService = notificationService;
        this.blockService = blockService;
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("Không thể gửi lời mời kết bạn cho chính mình!");
        }

        // 0. Một trong hai đã chặn người kia -> không cho gửi (câu báo lỗi chung chung, không lộ ai chặn ai)
        if (blockService.isBlockedEitherWay(fromUserId, toUserId)) {
            throw new RuntimeException("Không thể gửi lời mời kết bạn cho người này!");
        }

        // 1. Kiểm tra nếu đã là bạn bè rồi thì không cho gửi nữa
        if (isFriend(fromUserId, toUserId)) {
            throw new RuntimeException("Hai người đã là bạn bè của nhau rồi!");
        }

        // 2. Kiểm tra nếu đã gửi lời mời trước đó rồi thì không cho gửi trùng
        if (friendRequestRepository.existsByFromUserIdAndToUserId(fromUserId, toUserId)) {
            throw new RuntimeException("Bạn đã gửi lời mời kết bạn cho người này rồi!");
        }

        FriendRequest friendRequest = new FriendRequest(fromUserId, toUserId, message);
        FriendRequest saved = friendRequestRepository.save(friendRequest);
        // Báo cho người nhận: "X đã gửi lời mời kết bạn" (referenceId = id lời mời để
        // bấm Chấp nhận / Từ chối)
        notificationService.notify(toUserId, fromUserId, NotificationType.FRIEND_REQUEST, saved.getId());
        log.info(">>> ĐÃ GỬI LỜI MỜI KẾT BẠN: from {} to {}", fromUserId, toUserId);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long currentUserId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findByIdAndToUserId(requestId, currentUserId)
                .orElse(null);
        if (friendRequest == null) {
            throw new RuntimeException("Lời mời không tồn tại hoặc bạn không có quyền với lời mời này!");
        }

        Long fromUserId = friendRequest.getFromUserId();

        // Tạo quan hệ bạn bè mới (@PrePersist sẽ tự đảo ID nhỏ hơn vào userAId)
        Friend friend = new Friend(fromUserId, currentUserId);
        friendRepository.save(friend);

        // Xóa lời mời kết bạn sau khi đã chấp nhận
        friendRequestRepository.delete(friendRequest);
        // Lời mời đã xử lý xong -> xoá thông báo "X đã gửi lời mời" của mình
        notificationService.deleteByReference(NotificationType.FRIEND_REQUEST, requestId);
        // Báo cho người đã gửi lời mời: "Y đã chấp nhận lời mời kết bạn"
        notificationService.notify(fromUserId, currentUserId, NotificationType.FRIEND_ACCEPTED, requestId);
        log.info(">>> ĐÃ CHẤP NHẬN KẾT BẠN: {} và {}", fromUserId, currentUserId);
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long currentUserId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findByIdAndToUserId(requestId, currentUserId)
                .orElse(null);
        if (friendRequest == null) {
            throw new RuntimeException("Lời mời không tồn tại hoặc bạn không có quyền với lời mời này!");
        }
        friendRequestRepository.delete(friendRequest);
        // Không báo cho người gửi (giống Facebook); chỉ xoá thông báo lời mời đã mất
        notificationService.deleteByReference(NotificationType.FRIEND_REQUEST, requestId);
        log.info(">>> ĐÃ TỪ CHỐI LỜI MỜI KẾT BẠN ID = {}", requestId);
    }

    @Override
    @Transactional
    public void unfriend(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new RuntimeException("Không thể hủy kết bạn với chính mình!");
        }

        // Chuẩn hóa: ID nhỏ là minId, ID lớn là maxId
        Long minId = Math.min(currentUserId, friendId);
        Long maxId = Math.max(currentUserId, friendId);

        Friend friend = friendRepository.findByUserAIdAndUserBId(minId, maxId).orElse(null);
        if (friend == null) {
            throw new RuntimeException("Hai người không phải bạn bè của nhau!");
        }

        friendRepository.delete(friend);
        log.info(">>> ĐÃ HỦY KẾT BẠN: {} và {}", currentUserId, friendId);
    }

    @Override
    public List<FriendRequest> getPendingRequests(Long currentUserId) {
        return friendRequestRepository.findByToUserId(currentUserId);
    }

    @Override
    public List<UserResponse> getFriends(Long currentUserId) {
        List<Long> listFriendIds = friendRepository.findFriendIdsByUserId(currentUserId);
        if (listFriendIds == null || listFriendIds.isEmpty()) {
            return List.of();
        }
        List<User> listFriends = userRepository.findAllById(listFriendIds);
        return listFriends.stream().map(UserResponse::fromUser).toList();
    }

    @Override
    public boolean isFriend(Long userAId, Long userBId) {
        if (userAId == null || userBId == null || userAId.equals(userBId)) {
            return false;
        }

        Long minId = Math.min(userAId, userBId);
        Long maxId = Math.max(userAId, userBId);

        return friendRepository.existsByUserAIdAndUserBId(minId, maxId);
    }

    @Override
    public List<FriendSearchResponse> searchFriends(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String q = keyword.trim();
        Set<Long> friendIds = new HashSet<>(friendRepository.findFriendIdsByUserId(userId));
        // Người có quan hệ chặn với mình (2 chiều) -> loại khỏi kết quả, 2 bên không tìm thấy nhau
        Set<Long> blockedIds = blockService.getBlockRelatedIds(userId);
        NativeQuery query = NativeQuery.builder()
                .withQuery(root -> root.bool(b -> {
                    // 1. BẮT BUỘC khớp tên, chia tầng điểm để ai khớp ĐỦ các từ đứng trên cùng:
                    // - đúng cả DẤU, đúng thứ tự (gõ "Lê Liêm" -> "Lê Liêm") x20
                    // - đúng cả DẤU, đủ mọi từ ("Liêm Lê Liêm") x5
                    // - đúng cụm, đúng thứ tự, bỏ qua dấu ("Lê Liểm", "Le Liem") x10
                    // - đủ mọi từ, cho phép gõ dở ("Lê Bá Liêm" khi gõ "le li") x5
                    // - đủ mọi từ, cho phép gõ sai ("le lime") x2
                    // - chỉ khớp 1 phần ("Lê Phương Sơn" khi gõ "le liem") x0.3 -> xuống cuối
                    b.must(m -> m.bool(name -> name
                            .should(s -> s.matchPhrase(x -> x.field("username.exact").query(q).boost(20f)))
                            .should(s -> s.match(x -> x.field("username.exact").query(q)
                                    .operator(Operator.And).boost(5f)))
                            .should(s -> s.matchPhrase(x -> x.field("username").query(q).boost(10f)))
                            .should(s -> s.match(x -> x.field("username.prefix").query(q)
                                    .operator(Operator.And).boost(5f)))
                            .should(s -> s.match(x -> x.field("username").query(q)
                                    .operator(Operator.And).fuzziness("AUTO").boost(2f)))
                            .should(s -> s.match(x -> x.field("username.prefix").query(q).boost(0.3f)))
                            .minimumShouldMatch("1")));
                    // 2. Không hiện chính mình
                    b.mustNot(n -> n.term(t -> t.field("id").value(userId)));
                    if (!blockedIds.isEmpty()) {
                        b.mustNot(n -> n.terms(t -> t.field("id")
                                .terms(v -> v.value(blockedIds.stream().map(FieldValue::of).toList()))));
                    }
                    // 3. Không bắt buộc: là bạn bè thì cộng thêm điểm -> xếp lên trên
                    if (!friendIds.isEmpty()) {
                        b.should(s -> s.terms(t -> t.field("id")
                                .terms(v -> v.value(friendIds.stream().map(FieldValue::of).toList()))
                                .boost(2f)));
                    }
                    return b;
                })).withMaxResults(20).build();
        // In câu query dạng JSON (giống hệt cái dán vào Kibana Dev Tools)
        log.info("[ES] Query tìm \"{}\": {}", q, query.getQuery());
        try {
            List<SearchHit<UserDocument>> hits = elasticsearchOperations.search(query, UserDocument.class)
                    .getSearchHits();
            // In từng kết quả kèm điểm để xem vì sao người này đứng trên người kia
            hits.forEach(h -> log.info("[ES]   {} | id={} | {}", h.getScore(), h.getContent().getId(),
                    h.getContent().getUsername()));
            List<UserDocument> docs = hits.stream().map(SearchHit::getContent).toList();

            // Lời mời đang chờ (bảng friend_requests: còn dòng = đang chờ)
            Set<Long> sentTo = friendRequestRepository.findByFromUserId(userId).stream()
                    .map(FriendRequest::getToUserId).collect(Collectors.toSet());
            // Người đã gửi lời mời cho mình -> id lời mời (để bấm "Chấp nhận" ngay trong kết quả tìm kiếm)
            Map<Long, Long> requestIdByFromUser = friendRequestRepository.findByToUserId(userId).stream()
                    .collect(Collectors.toMap(FriendRequest::getFromUserId, FriendRequest::getId, (a, b) -> a));
            Set<Long> receivedFrom = requestIdByFromUser.keySet();

            return docs.stream()
                    .map(d -> new FriendSearchResponse(d.getId(), d.getUsername(), d.getAvatarUrl(),
                            relationOf(d.getId(), friendIds, sentTo, receivedFrom),
                            requestIdByFromUser.get(d.getId())))
                    .toList();
        } catch (Exception e) {
            // ES lỗi: ghi log, trả rỗng thay vì lỗi 500
            log.warn("[ES] Tìm kiếm người dùng thất bại: {}", e.getMessage());
            return List.of();
        }
    }

    private FriendRelation relationOf(Long otherId, Set<Long> friendIds, Set<Long> sentTo, Set<Long> receivedFrom) {
        if (friendIds.contains(otherId))
            return FriendRelation.FRIEND;
        if (sentTo.contains(otherId))
            return FriendRelation.REQUEST_SENT;
        if (receivedFrom.contains(otherId))
            return FriendRelation.REQUEST_RECEIVED;
        return FriendRelation.NONE;
    }
}
