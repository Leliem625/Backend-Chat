package com.example.demo.dto;

import java.util.List;

import com.example.demo.entity.MessageType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SendFileMessageRequest(
                @NotNull(message = "conversationId không được để trống") Long conversationId,

                String content, // Lời nhắn kèm theo ảnh (nếu có)

                @NotEmpty(message = "Danh sách file đính kèm không được để trống") List<UploadResponse> attachments,
                MessageType type)

{
}