package com.example.demo.dto;

import com.example.demo.entity.Organization;
import com.example.demo.entity.UserWork;

public record WorkResponse(
    Long id,
    OrganizationResponse company,
    String position,
    Integer fromYear,
    Integer toYear  // null = đang làm
) {
    public static WorkResponse from(UserWork w, Organization company) {
        return new WorkResponse(w.getId(), OrganizationResponse.from(company), w.getPosition(),
                w.getFromYear(), w.getToYear());
    }
}
