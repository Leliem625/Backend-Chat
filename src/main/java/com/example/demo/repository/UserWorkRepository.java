package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.UserWork;

@Repository
public interface UserWorkRepository extends JpaRepository<UserWork, Long> {

    // Công việc của 1 người, mới nhất trước (đang làm -> to_year null xếp đầu)
    List<UserWork> findByUserIdOrderByToYearDescFromYearDesc(Long userId);

    // Chỉ sửa / xoá được dòng của chính mình
    Optional<UserWork> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
