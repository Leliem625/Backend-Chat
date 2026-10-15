package com.example.demo.search;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

// Đồng bộ user từ MySQL (dữ liệu gốc) sang Elasticsearch (bản sao để tìm kiếm)
// - Mỗi 2 phút: chỉ đẩy user mới tạo / vừa sửa (theo updated_at)
// - 3h sáng mỗi ngày: đẩy lại toàn bộ + xoá user không còn trong MySQL
@Component
public class UserSearchSyncJob {

    private static final Logger log = LoggerFactory.getLogger(UserSearchSyncJob.class);
    private static final int BATCH_SIZE = 500;

    private final UserRepository userRepository;
    private final UserSearchRepository userSearchRepository;

    // Mốc của lần đồng bộ thành công gần nhất. null = chưa đồng bộ lần nào từ lúc
    // bật app
    // -> lần đầu chạy sẽ đồng bộ toàn bộ
    private LocalDateTime lastSyncAt = null;

    public UserSearchSyncJob(UserRepository userRepository, UserSearchRepository userSearchRepository) {
        this.userRepository = userRepository;
        this.userSearchRepository = userSearchRepository;
    }

    // initialDelay: chờ app khởi động xong 10s rồi mới chạy lần đầu
    // fixedDelay: lần sau chỉ bắt đầu khi lần trước đã CHẠY XONG + 2 phút -> không
    // bao giờ chồng nhau
    @Scheduled(initialDelay = 10_000, fixedDelayString = "${search.sync.changed-delay-ms}")
    public void syncChangedUsers() {
        // Lấy mốc TRƯỚC khi đọc DB: user nào được sửa trong lúc đang đồng bộ sẽ được
        // lần sau bắt lại
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            if (lastSyncAt == null) {
                int total = reindexAll();
                log.info("[ES] Đồng bộ lần đầu: {} user", total);
            } else {
                // Lùi 5 giây để không sót user được lưu đúng lúc chuyển mốc
                List<User> changed = userRepository.findByUpdatedAtAfter(lastSyncAt.minusSeconds(5));
                if (!changed.isEmpty()) {
                    userSearchRepository.saveAll(changed.stream().map(UserDocument::fromUser).toList());
                    log.info("[ES] Đồng bộ {} user thay đổi", changed.size());
                }
            }
            // Chỉ dời mốc khi thành công -> ES lỗi thì lần sau tự làm lại từ mốc cũ
            lastSyncAt = startedAt;
        } catch (Exception e) {
            // ES tắt / lỗi mạng: chỉ ghi log, KHÔNG được làm sập app
            log.warn("[ES] Đồng bộ user thay đổi thất bại: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${search.sync.full-cron}", zone = "Asia/Ho_Chi_Minh")
    public void fullReindex() {
        try {
            Set<Long> existingIds = new HashSet<>();
            int total = reindexAll(existingIds);

            // Xoá document của user đã bị xoá khỏi MySQL
            List<Long> staleIds = StreamSupport.stream(userSearchRepository.findAll().spliterator(), false)
                    .map(UserDocument::getId)
                    .filter(id -> !existingIds.contains(id))
                    .toList();
            if (!staleIds.isEmpty()) {
                userSearchRepository.deleteAllById(staleIds);
            }
            log.info("[ES] Reindex toàn bộ: {} user, xoá {} user cũ", total, staleIds.size());
        } catch (Exception e) {
            log.warn("[ES] Reindex toàn bộ thất bại: {}", e.getMessage());
        }
    }

    private int reindexAll() {
        return reindexAll(new HashSet<>());
    }

    // Đọc MySQL theo từng trang 500 user rồi đẩy sang ES, không load hết vào RAM
    // một lúc
    private int reindexAll(Set<Long> collectedIds) {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));
        int total = 0;
        Page<User> page;
        do {
            page = userRepository.findAll(pageable);
            List<UserDocument> docs = page.getContent().stream().map(UserDocument::fromUser).toList();
            if (!docs.isEmpty()) {
                userSearchRepository.saveAll(docs);
            }
            docs.forEach(d -> collectedIds.add(d.getId()));
            total += docs.size();
            pageable = page.nextPageable();
        } while (page.hasNext());
        return total;
    }
}