package com.example.demo.entity;

public enum MessageType {
    TEXT,   // Tin nhắn văn bản (có thể chứa link)
    IMAGE,  // Tin nhắn có ảnh đính kèm
    FILE,   // Tin nhắn có tệp đính kèm
    SYSTEM  // Tin hệ thống (vd: "A đã thêm B vào nhóm")
}
