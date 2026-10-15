package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.EducationRequest;
import com.example.demo.dto.EducationResponse;
import com.example.demo.dto.OrganizationResponse;
import com.example.demo.dto.UpdateInterestsRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.dto.WorkRequest;
import com.example.demo.dto.WorkResponse;
import com.example.demo.entity.OrganizationType;

public interface ProfileService {

    // 1. Xem trang cá nhân (của mình hoặc người khác), kèm số bạn bè, bạn chung, quan hệ
    UserProfileResponse getProfile(Long viewerId, Long userId);

    // 2. Sửa thông tin cơ bản của mình
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    // 3. Học vấn
    EducationResponse addEducation(Long userId, EducationRequest request);

    EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request);

    void deleteEducation(Long userId, Long educationId);

    // 4. Công việc
    WorkResponse addWork(Long userId, WorkRequest request);

    WorkResponse updateWork(Long userId, Long workId, WorkRequest request);

    void deleteWork(Long userId, Long workId);

    // 5. Sở thích (ghi đè toàn bộ danh sách)
    List<String> updateInterests(Long userId, UpdateInterestsRequest request);

    // 6. Gợi ý trường / công ty khi gõ
    List<OrganizationResponse> searchOrganizations(OrganizationType type, String keyword, int limit);
}
