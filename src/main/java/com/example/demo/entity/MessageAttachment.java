package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "message_attachments",
    indexes = {
        // Index để lấy nhanh các file đính kèm của 1 tin nhắn
        @Index(name = "idx_attachment_message", columnList = "message_id"),
        // Index cho mục "Ảnh / File đã gửi" của 1 cuộc trò chuyện (lọc theo loại, mới nhất trước)
        @Index(name = "idx_attachment_conversation_type", columnList = "conversation_id, resource_type, id")
    }
)
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    // Lưu thêm conversationId để lấy ảnh / file của cuộc trò chuyện mà không cần JOIN bảng messages
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    // Đường dẫn https của file trên Cloudinary
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    // ID của file trên Cloudinary (dùng khi cần xoá file)
    @Column(name = "public_id", nullable = false)
    private String publicId;

    // image | video | raw (Cloudinary cần giá trị này khi xoá file)
    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // Tên file gốc để hiển thị
    @Column(name = "file_name")
    private String fileName;

    // Dung lượng (byte)
    @Column(name = "size")
    private Long size;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public MessageAttachment() {
    }

    public MessageAttachment(Long messageId, Long conversationId, String url, String publicId, String resourceType, String mimeType,
            String fileName, Long size) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.url = url;
        this.publicId = publicId;
        this.resourceType = resourceType;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.size = size;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getter và Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "MessageAttachment{" +
                "id=" + id +
                ", messageId=" + messageId +
                ", conversationId=" + conversationId +
                ", url='" + url + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", size=" + size +
                '}';
    }
}
