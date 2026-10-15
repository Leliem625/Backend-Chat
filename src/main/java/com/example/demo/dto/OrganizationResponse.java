package com.example.demo.dto;

import com.example.demo.entity.Organization;
import com.example.demo.entity.OrganizationType;

// 1 trường / công ty trong danh mục (dùng cho gợi ý khi gõ và hiển thị trong hồ sơ)
public record OrganizationResponse(
    Long id,
    String name,
    OrganizationType type,
    String city,
    boolean verified
) {
    public static OrganizationResponse from(Organization o) {
        return o == null ? null : new OrganizationResponse(o.getId(), o.getName(), o.getType(), o.getCity(), o.isVerified());
    }
}
