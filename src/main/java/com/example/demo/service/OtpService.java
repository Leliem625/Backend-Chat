package com.example.demo.service;

public interface OtpService {
    void sendOtpByEmail(String email);

    boolean verifyOtp(Integer otp, String email);

}
