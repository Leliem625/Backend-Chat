package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Ghi đè toàn bộ danh sách sở thích (gửi [] để xoá hết)
public record UpdateInterestsRequest(
    @NotNull(message = "interests không được để trống")
    @Size(max = 20, message = "Tối đa 20 sở thích")
    List<@Size(max = 50, message = "Mỗi sở thích tối đa 50 ký tự") String> interests
) {}
