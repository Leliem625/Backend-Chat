package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UploadResponse;

public interface FileStorageService {

    UploadResponse upload(Long userId, MultipartFile file);

    void delete(String publicId, String resourceType);
}
