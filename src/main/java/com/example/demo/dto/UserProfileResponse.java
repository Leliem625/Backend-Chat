package com.example.demo.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.entity.Gender;
import com.fasterxml.jackson.annotation.JsonInclude;

// Trang cá nhân. email chỉ trả khi xem hồ sơ của chính mình
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
    Long id,
    String username,
    String email,
    String avatarUrl,
    String bio,
    Gender gender,
    LocalDate birthday,
    String hometown,
    String currentCity,
    List<EducationResponse> educations,
    List<WorkResponse> works,
    List<String> interests,
    long friendCount,
    long mutualFriendCount,      // số bạn chung (hồ sơ của mình -> 0)
    boolean me,                  // true = hồ sơ của chính mình -> hiện nút "Chỉnh sửa"
    FriendRelation relation,     // quan hệ với mình (hồ sơ của mình -> null)
    Long requestId               // REQUEST_RECEIVED: id lời mời để bấm "Chấp nhận"
) {}
