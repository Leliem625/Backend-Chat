package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.OrganizationResponse;
import com.example.demo.entity.OrganizationType;
import com.example.demo.service.ProfileService;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final ProfileService profileService;

    public OrganizationController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // GET /api/organization/search?type=SCHOOL&q=bach khoa : gợi ý khi gõ tên trường / công ty
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> search(
            @RequestParam OrganizationType type,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công!",
                profileService.searchOrganizations(type, q, limit)));
    }
}
