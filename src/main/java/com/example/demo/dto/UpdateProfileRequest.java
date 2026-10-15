package com.example.demo.dto;

import java.time.LocalDate;

import com.example.demo.entity.Gender;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

// Sửa thông tin cơ bản của mình. Gửi đủ các trường: trường nào null / rỗng nghĩa là bỏ trống
public record UpdateProfileRequest(
    @Size(max = 50, message = "Tên tối đa 50 ký tự") String username,
    @Size(max = 500, message = "Giới thiệu tối đa 500 ký tự") String bio,
    Gender gender,
    @Past(message = "Ngày sinh phải ở quá khứ") LocalDate birthday,   // "2003-05-20"
    @Size(max = 100, message = "Quê quán tối đa 100 ký tự") String hometown,
    @Size(max = 100, message = "Nơi sống tối đa 100 ký tự") String currentCity
) {}
