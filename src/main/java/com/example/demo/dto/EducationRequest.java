package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// Thêm / sửa 1 dòng học vấn.
// Chọn trường từ gợi ý -> gửi organizationId; trường chưa có -> gửi organizationName (+ city) để tạo mới
public record EducationRequest(
    Long organizationId,
    @Size(max = 150, message = "Tên trường tối đa 150 ký tự") String organizationName,
    @Size(max = 100, message = "Thành phố tối đa 100 ký tự") String city,
    @Size(max = 100, message = "Chuyên ngành tối đa 100 ký tự") String major,
    @Min(value = 1950, message = "Năm không hợp lệ") @Max(value = 2100, message = "Năm không hợp lệ") Integer fromYear,
    @Min(value = 1950, message = "Năm không hợp lệ") @Max(value = 2100, message = "Năm không hợp lệ") Integer toYear
) {}
