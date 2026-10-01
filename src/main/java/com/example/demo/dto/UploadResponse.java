package com.example.demo.dto;

public record UploadResponse(
    String url,          // Đường dẫn https để hiển thị / tải file
    String publicId,     // ID của file trên Cloudinary (dùng khi cần xoá)
    String resourceType, // image | video | raw
    String mimeType,
    String name,         // Tên file gốc để hiển thị
    Long size            // Dung lượng (byte)
) {}
