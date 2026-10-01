package com.example.demo.service;

import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request);

    void logout(String refreshToken);

    String refreshToken(String refreshToken);

    UserResponse getMe(Long userId);

    UserResponse changePassword(Long userId, String passwordOld, String passwordNew);

    UserResponse forgotPassword(ForgotPasswordRequest request);
}
