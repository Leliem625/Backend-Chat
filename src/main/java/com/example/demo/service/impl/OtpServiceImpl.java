package com.example.demo.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Otp;
import com.example.demo.entity.User;
import com.example.demo.repository.OtpRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OtpServiceImpl(OtpRepository otpRepository, UserRepository userRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void sendOtpByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không chính xác hoặc chưa tồn tại!"));

        // 1. Sinh mã OTP 6 chữ số ngẫu nhiên (ví dụ 102938)
        SecureRandom random = new SecureRandom();
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // 2. Lưu OTP mới vào database với hạn dùng 5 phút
        Otp otp = new Otp(email, otpCode, LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otp);

        // 3. Gửi email chứa mã OTP đến người dùng
        emailService.sendOtpEmail(email, otpCode);
    }

    @Override
    @Transactional
    public boolean verifyOtp(Integer otp, String email) {
        Otp otpVerify = otpRepository.findOtpNewByEmail(email)
                .orElseThrow(() -> new RuntimeException("Chưa có mã Otp nào được gửi đi!"));
        if (otpVerify.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn, vui lòng lấy mã mới!");
        }

        if (!otpVerify.getOtp().equals(String.valueOf(otp))) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }
        otpVerify.setIsUsed(true);
        otpRepository.save(otpVerify);
        return true;
    }

}
