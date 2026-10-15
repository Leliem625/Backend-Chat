package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.EducationRequest;
import com.example.demo.dto.EducationResponse;
import com.example.demo.dto.UpdateInterestsRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.dto.WorkRequest;
import com.example.demo.dto.WorkResponse;
import com.example.demo.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // GET /api/user/me/profile: hồ sơ của mình (đầy đủ, kèm email)
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công!", profileService.getProfile(userId, userId)));
    }

    // GET /api/user/{id}/profile: hồ sơ người khác (kèm số bạn chung + quan hệ với mình)
    @GetMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công!", profileService.getProfile(userId, id)));
    }

    // PUT /api/user/me/profile: sửa thông tin cơ bản
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công!",
                profileService.updateProfile(userId, request)));
    }

    // ----- Học vấn -----
    @PostMapping("/me/educations")
    public ResponseEntity<ApiResponse<EducationResponse>> addEducation(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody EducationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã thêm học vấn!", profileService.addEducation(userId, request)));
    }

    @PutMapping("/me/educations/{id}")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(@RequestAttribute("userId") Long userId,
            @PathVariable Long id, @Valid @RequestBody EducationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật học vấn!",
                profileService.updateEducation(userId, id, request)));
    }

    @DeleteMapping("/me/educations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        profileService.deleteEducation(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá học vấn!", null));
    }

    // ----- Công việc -----
    @PostMapping("/me/works")
    public ResponseEntity<ApiResponse<WorkResponse>> addWork(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody WorkRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã thêm công việc!", profileService.addWork(userId, request)));
    }

    @PutMapping("/me/works/{id}")
    public ResponseEntity<ApiResponse<WorkResponse>> updateWork(@RequestAttribute("userId") Long userId,
            @PathVariable Long id, @Valid @RequestBody WorkRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật công việc!",
                profileService.updateWork(userId, id, request)));
    }

    @DeleteMapping("/me/works/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWork(@RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        profileService.deleteWork(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá công việc!", null));
    }

    // ----- Sở thích -----
    @PutMapping("/me/interests")
    public ResponseEntity<ApiResponse<List<String>>> updateInterests(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateInterestsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật sở thích!",
                profileService.updateInterests(userId, request)));
    }
}
