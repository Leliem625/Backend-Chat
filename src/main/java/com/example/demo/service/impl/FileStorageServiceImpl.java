package com.example.demo.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.UploadResponse;
import com.example.demo.service.FileStorageService;
import com.example.demo.util.ParseUtils;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Set<String> FILE_TYPES = Set.of(
            "application/pdf",
            "application/zip",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final Cloudinary cloudinary;

    public FileStorageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public UploadResponse upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File không được để trống!");
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && IMAGE_TYPES.contains(contentType);
        if (!isImage && (contentType == null || !FILE_TYPES.contains(contentType))) {
            throw new RuntimeException("Loại file không được hỗ trợ!");
        }
        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Ảnh không được vượt quá 5MB!");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File không được vượt quá 20MB!");
        }

        // Tự đặt tên file trên Cloudinary, không dùng tên gốc của người dùng.
        // File thường (raw) phải có đuôi trong public_id thì link tải về mới có đuôi.
        String publicId = UUID.randomUUID().toString();
        if (!isImage) {
            publicId += extensionOf(file.getOriginalFilename());
        }
        String resourceType = isImage ? "image" : "raw";

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "chat/" + userId,
                    "public_id", publicId,
                    "resource_type", resourceType));

            return new UploadResponse(
                    ParseUtils.toString(result.get("secure_url")),
                    ParseUtils.toString(result.get("public_id")),
                    resourceType,
                    contentType,
                    file.getOriginalFilename(),
                    file.getSize());
        } catch (IOException e) {
            log.error("Lỗi khi tải file lên Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tải file lên, vui lòng thử lại!");
        }
    }

    @Override
    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            log.error("Lỗi khi xoá file {} trên Cloudinary: {}", publicId, e.getMessage(), e);
            throw new RuntimeException("Không thể xoá file!");
        }
    }

    // Lấy đuôi file (vd ".pdf"), chỉ giữ chữ và số để tránh ký tự lạ trong public_id
    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        if (ext.length() > 10 || !ext.matches("[a-z0-9]+")) {
            return "";
        }
        return "." + ext;
    }
}
