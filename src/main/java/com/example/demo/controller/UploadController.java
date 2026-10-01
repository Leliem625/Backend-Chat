package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UploadResponse;
import com.example.demo.service.FileStorageService;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        UploadResponse response = fileStorageService.upload(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Tải file thành công!", response));
    }
}
