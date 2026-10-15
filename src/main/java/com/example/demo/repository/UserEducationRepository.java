package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.UserEducation;

@Repository
public interface UserEducationRepository extends JpaRepository<UserEducation, Long> {

    // Học vấn của 1 người, mới nhất trước (đang học -> to_year null xếp đầu)
    List<UserEducation> findByUserIdOrderByToYearDescFromYearDesc(Long userId);

    // Chỉ sửa / xoá được dòng của chính mình
    Optional<UserEducation> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
