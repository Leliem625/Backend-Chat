package com.example.demo.dto;

import com.example.demo.entity.Organization;
import com.example.demo.entity.UserEducation;

public record EducationResponse(
    Long id,
    OrganizationResponse school,
    String major,
    Integer fromYear,
    Integer toYear  // null = đang học
) {
    public static EducationResponse from(UserEducation e, Organization school) {
        return new EducationResponse(e.getId(), OrganizationResponse.from(school), e.getMajor(),
                e.getFromYear(), e.getToYear());
    }
}
