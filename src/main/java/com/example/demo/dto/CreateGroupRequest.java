package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateGroupRequest(
    @NotBlank(message = "Tên nhóm không được để trống")
    String name,

    @NotEmpty(message = "Nhóm phải có ít nhất một thành viên")
    List<Long> memberIds
) {}
