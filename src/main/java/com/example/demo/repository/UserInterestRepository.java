package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.UserInterest;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserIdOrderByIdAsc(Long userId);

    // Ghi đè danh sách sở thích: xoá hết rồi thêm lại
    @Modifying
    @Query("DELETE FROM UserInterest i WHERE i.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
