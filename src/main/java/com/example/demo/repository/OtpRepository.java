package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Otp;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    // 1. Tìm mã OTP mới nhất chưa sử dụng của email đó
    Optional<Otp> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    // 2. Tìm chính xác theo cặp (email, otp) chưa sử dụng
    Optional<Otp> findByEmailAndOtpAndIsUsedFalse(String email, String otp);

    // 3. Xóa các mã OTP cũ của email
    void deleteByEmail(String email);

    boolean existsByOtp(Integer otp);

    @Query(value = """
            SELECT * from otps where email = :email ORDER BY created_at DESC limit 1
            """, nativeQuery = true)
    Optional<Otp> findOtpNewByEmail(@Param("email") String email);
}
