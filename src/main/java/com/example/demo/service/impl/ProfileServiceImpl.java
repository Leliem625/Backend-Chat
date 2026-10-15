package com.example.demo.service.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.EducationRequest;
import com.example.demo.dto.EducationResponse;
import com.example.demo.dto.FriendRelation;
import com.example.demo.dto.OrganizationResponse;
import com.example.demo.dto.UpdateInterestsRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.dto.WorkRequest;
import com.example.demo.dto.WorkResponse;
import com.example.demo.entity.FriendRequest;
import com.example.demo.entity.Organization;
import com.example.demo.entity.OrganizationType;
import com.example.demo.entity.User;
import com.example.demo.entity.UserEducation;
import com.example.demo.entity.UserInterest;
import com.example.demo.entity.UserWork;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.FriendRequestRepository;
import com.example.demo.repository.OrganizationRepository;
import com.example.demo.repository.UserEducationRepository;
import com.example.demo.repository.UserInterestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserWorkRepository;
import com.example.demo.service.BlockService;
import com.example.demo.service.ProfileService;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final int MAX_EDUCATIONS = 10;
    private static final int MAX_WORKS = 10;
    private static final int MAX_SUGGESTIONS = 20;

    private final UserRepository userRepository;
    private final UserEducationRepository educationRepository;
    private final UserWorkRepository workRepository;
    private final UserInterestRepository interestRepository;
    private final OrganizationRepository organizationRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final BlockService blockService;

    public ProfileServiceImpl(UserRepository userRepository, UserEducationRepository educationRepository,
            UserWorkRepository workRepository, UserInterestRepository interestRepository,
            OrganizationRepository organizationRepository, FriendRepository friendRepository,
            FriendRequestRepository friendRequestRepository, BlockService blockService) {
        this.userRepository = userRepository;
        this.educationRepository = educationRepository;
        this.workRepository = workRepository;
        this.interestRepository = interestRepository;
        this.organizationRepository = organizationRepository;
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.blockService = blockService;
    }

    // ================= 1. Xem trang cá nhân =================

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long viewerId, Long userId) {
        boolean isMe = viewerId.equals(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        // Chặn nhau -> coi như không tồn tại (không lộ là bị chặn)
        if (!isMe && blockService.isBlockedEitherWay(viewerId, userId)) {
            throw new RuntimeException("Không tìm thấy người dùng!");
        }

        // Học vấn + công việc: lấy hết trường / công ty liên quan bằng 1 query
        List<UserEducation> educations = educationRepository.findByUserIdOrderByToYearDescFromYearDesc(userId);
        List<UserWork> works = workRepository.findByUserIdOrderByToYearDescFromYearDesc(userId);
        Set<Long> orgIds = new HashSet<>();
        educations.forEach(e -> orgIds.add(e.getOrganizationId()));
        works.forEach(w -> orgIds.add(w.getOrganizationId()));
        Map<Long, Organization> orgs = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity()));

        List<String> interests = interestRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(UserInterest::getName).toList();

        // Số bạn bè đếm lúc trả về (không lưu sẵn để khỏi lệch khi kết bạn / huỷ kết bạn)
        Set<Long> friendIds = new HashSet<>(friendRepository.findFriendIdsByUserId(userId));
        long mutualCount = 0;
        FriendRelation relation = null;
        Long requestId = null;
        if (!isMe) {
            Set<Long> myFriendIds = new HashSet<>(friendRepository.findFriendIdsByUserId(viewerId));
            mutualCount = friendIds.stream().filter(myFriendIds::contains).count();

            if (friendIds.contains(viewerId)) {
                relation = FriendRelation.FRIEND;
            } else if (friendRequestRepository.existsByFromUserIdAndToUserId(viewerId, userId)) {
                relation = FriendRelation.REQUEST_SENT;
            } else {
                FriendRequest received = friendRequestRepository.findByFromUserIdAndToUserId(userId, viewerId)
                        .orElse(null);
                relation = received != null ? FriendRelation.REQUEST_RECEIVED : FriendRelation.NONE;
                requestId = received != null ? received.getId() : null;
            }
        }

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                isMe ? user.getEmail() : null,
                user.getAvatarUrl(),
                user.getBio(),
                user.getGender(),
                user.getBirthday(),
                user.getHometown(),
                user.getCurrentCity(),
                educations.stream().map(e -> EducationResponse.from(e, orgs.get(e.getOrganizationId()))).toList(),
                works.stream().map(w -> WorkResponse.from(w, orgs.get(w.getOrganizationId()))).toList(),
                interests,
                friendIds.size(),
                mutualCount,
                isMe,
                relation,
                requestId);
    }

    // ================= 2. Sửa thông tin cơ bản =================

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        // Tên không được để trống: không gửi thì giữ tên cũ
        String username = clean(request.username());
        if (username != null) {
            user.setUsername(username);
        }
        user.setBio(clean(request.bio()));
        user.setGender(request.gender());
        user.setBirthday(request.birthday());
        user.setHometown(clean(request.hometown()));
        user.setCurrentCity(clean(request.currentCity()));
        // save -> @PreUpdate đổi updated_at -> job đồng bộ Elasticsearch tự cập nhật tên / thành phố mới
        userRepository.save(user);
        return getProfile(userId, userId);
    }

    // ================= 3. Học vấn =================

    @Override
    @Transactional
    public EducationResponse addEducation(Long userId, EducationRequest request) {
        if (educationRepository.countByUserId(userId) >= MAX_EDUCATIONS) {
            throw new RuntimeException("Chỉ được thêm tối đa " + MAX_EDUCATIONS + " trường học!");
        }
        UserEducation education = new UserEducation();
        education.setUserId(userId);
        return saveEducation(userId, education, request);
    }

    @Override
    @Transactional
    public EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request) {
        UserEducation education = educationRepository.findByIdAndUserId(educationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học vấn!"));
        return saveEducation(userId, education, request);
    }

    @Override
    @Transactional
    public void deleteEducation(Long userId, Long educationId) {
        UserEducation education = educationRepository.findByIdAndUserId(educationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học vấn!"));
        educationRepository.delete(education);
    }

    private EducationResponse saveEducation(Long userId, UserEducation education, EducationRequest request) {
        validateYears(request.fromYear(), request.toYear());
        Organization school = resolveOrganization(OrganizationType.SCHOOL, request.organizationId(),
                request.organizationName(), request.city(), userId);
        education.setOrganizationId(school.getId());
        education.setMajor(clean(request.major()));
        education.setFromYear(request.fromYear());
        education.setToYear(request.toYear());
        return EducationResponse.from(educationRepository.save(education), school);
    }

    // ================= 4. Công việc =================

    @Override
    @Transactional
    public WorkResponse addWork(Long userId, WorkRequest request) {
        if (workRepository.countByUserId(userId) >= MAX_WORKS) {
            throw new RuntimeException("Chỉ được thêm tối đa " + MAX_WORKS + " nơi làm việc!");
        }
        UserWork work = new UserWork();
        work.setUserId(userId);
        return saveWork(userId, work, request);
    }

    @Override
    @Transactional
    public WorkResponse updateWork(Long userId, Long workId, WorkRequest request) {
        UserWork work = workRepository.findByIdAndUserId(workId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin công việc!"));
        return saveWork(userId, work, request);
    }

    @Override
    @Transactional
    public void deleteWork(Long userId, Long workId) {
        UserWork work = workRepository.findByIdAndUserId(workId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin công việc!"));
        workRepository.delete(work);
    }

    private WorkResponse saveWork(Long userId, UserWork work, WorkRequest request) {
        validateYears(request.fromYear(), request.toYear());
        Organization company = resolveOrganization(OrganizationType.COMPANY, request.organizationId(),
                request.organizationName(), request.city(), userId);
        work.setOrganizationId(company.getId());
        work.setPosition(clean(request.position()));
        work.setFromYear(request.fromYear());
        work.setToYear(request.toYear());
        return WorkResponse.from(workRepository.save(work), company);
    }

    // ================= 5. Sở thích =================

    @Override
    @Transactional
    public List<String> updateInterests(Long userId, UpdateInterestsRequest request) {
        // Bỏ khoảng trắng thừa, bỏ chuỗi rỗng, bỏ trùng (giữ thứ tự người dùng nhập)
        Set<String> names = new LinkedHashSet<>();
        for (String raw : request.interests()) {
            String name = clean(raw);
            if (name != null && names.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
                names.add(name);
            }
        }
        interestRepository.deleteAllByUserId(userId);
        interestRepository.saveAll(names.stream().map(n -> new UserInterest(userId, n)).toList());
        return List.copyOf(names);
    }

    // ================= 6. Gợi ý trường / công ty =================

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> searchOrganizations(OrganizationType type, String keyword, int limit) {
        String q = clean(keyword);
        if (q == null) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), MAX_SUGGESTIONS);
        return organizationRepository.search(type, q, PageRequest.of(0, size)).stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    // Chọn từ gợi ý (organizationId) hoặc gõ tên mới (organizationName + city):
    // tên + thành phố đã có thì dùng lại, chưa có thì tạo mới (chưa xác minh) - giống "Trang" trên Facebook
    private Organization resolveOrganization(OrganizationType type, Long organizationId, String name,
            String city, Long userId) {
        if (organizationId != null) {
            return organizationRepository.findByIdAndType(organizationId, type)
                    .orElseThrow(() -> new RuntimeException(type == OrganizationType.SCHOOL
                            ? "Trường học không tồn tại!" : "Công ty không tồn tại!"));
        }
        String cleanName = clean(name);
        if (cleanName == null) {
            throw new RuntimeException(type == OrganizationType.SCHOOL
                    ? "Vui lòng chọn hoặc nhập tên trường!" : "Vui lòng chọn hoặc nhập tên công ty!");
        }
        String cleanCity = clean(city);
        List<Organization> same = organizationRepository.findSame(type, cleanName, cleanCity);
        if (!same.isEmpty()) {
            return same.get(0);
        }
        return organizationRepository.save(new Organization(cleanName, type, cleanCity, userId));
    }

    private void validateYears(Integer fromYear, Integer toYear) {
        if (fromYear != null && toYear != null && toYear < fromYear) {
            throw new RuntimeException("Năm kết thúc phải sau năm bắt đầu!");
        }
    }

    // Bỏ khoảng trắng 2 đầu, chuỗi rỗng coi như không có
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }
}
