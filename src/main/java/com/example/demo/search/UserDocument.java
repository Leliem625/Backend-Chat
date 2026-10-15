package com.example.demo.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.example.demo.entity.User;

// Bản sao của User trong Elasticsearch, chỉ chứa những gì cần để tìm kiếm & hiển thị kết quả
@Document(indexName = "users")
@Setting(settingPath = "es/user-settings.json")
public class UserDocument {

    @Id
    private Long id;

    // 1 tên lưu thành 2 field:
    // - username : tìm theo từ, bỏ dấu ("liem" khớp "Liêm")
    // - username.prefix : tìm khi đang gõ dở ("li" khớp "Liêm")
    // searchAnalyzer = vi_folding: từ khoá người dùng gõ KHÔNG bị cắt n-gram,
    // nếu không "liem" sẽ bị cắt thành "l" và khớp với mọi tên có chữ l
    // username.exact: giữ nguyên dấu (chỉ chuyển chữ thường) -> gõ "Lê Liêm" thì người tên
    // đúng "Lê Liêm" được cộng điểm, đứng trên "Lê Liểm" / "Le Liem"
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vi_folding"), otherFields = {
            @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "vi_prefix", searchAnalyzer = "vi_folding"),
            @InnerField(suffix = "exact", type = FieldType.Text, analyzer = "vi_exact") })
    private String username;

    // Chỉ để hiển thị, không tìm theo -> không cần đánh chỉ mục
    @Field(type = FieldType.Keyword, index = false)
    private String avatarUrl;

    public UserDocument() {
    }

    public UserDocument(Long id, String username, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public static UserDocument fromUser(User user) {
        return new UserDocument(user.getId(), user.getUsername(), user.getAvatarUrl());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}