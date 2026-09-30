package com.example.demo.service.impl;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Session;
import com.example.demo.entity.User;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        // Mã hóa mật khẩu, không lưu trực tiếp raw password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Tạo User entity và lưu xuống DB
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(hashedPassword);
        user.setBio(request.getBio());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {
            throw new RuntimeException("User not found!");
        }
        // 2. So khớp mật khẩu nhập vào với mật khẩu băm trong DB
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // 3. Tạo Access Token (24h) và Refresh Token (7 ngày) bằng JWT từ userId
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 4. Lưu Refresh Token vào bảng sessions trong DB
        Session session = new Session(user.getId(), refreshToken, LocalDateTime.now().plusDays(7));
        sessionRepository.save(session);
        log.info(">>> ĐÃ TẠO VÀ LƯU REFRESH TOKEN VÀO BẢNG SESSIONS CHO USER ID = {}", user.getId());

        // 5. Trả về thông tin user kèm cả Access Token và Refresh Token
        return UserResponse.fromUser(user, accessToken, refreshToken);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRepository.deleteByRefreshToken(refreshToken);
            log.info(">>> ĐÃ XÓA REFRESH TOKEN KHỎI DATABASE: {}", refreshToken);
        }
    }

    @Override
    public String refreshToken(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken).orElse(null);
        if (session == null) {
            throw new RuntimeException("Token không tồn tại!");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session); // Xóa luôn session đã hết hạn khỏi DB
            throw new RuntimeException("Token đã hết hạn!");
        }

        String accessToken = jwtService.generateAccessToken(session.getUserId());
        return accessToken;
    }

    @Override
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User khong ton tai!");
        }
        return UserResponse.fromUser(user);
    }
}
