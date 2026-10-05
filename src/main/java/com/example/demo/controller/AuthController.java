package com.example.demo.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.SendOtpRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.OtpService;
import com.example.demo.service.UserService;
import com.example.demo.util.ParseUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final UserService userService;
        private final OtpService otpService;
        private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7); // 7 ngày

        public AuthController(UserService userService, OtpService otpService) {
                this.userService = userService;
                this.otpService = otpService;
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
                UserResponse response = userService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("User registered successfully", response));
        }

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
                UserResponse response = userService.login(request);

                // Tạo Cookie "logged"
                ResponseCookie loggedCookie = ResponseCookie.from("logged", "1")
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("None")
                                .path("/")
                                .maxAge(REFRESH_TOKEN_TTL)
                                .build();

                // Tạo Cookie "refreshToken"
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("None")
                                .path("/")
                                .maxAge(REFRESH_TOKEN_TTL)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, loggedCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(ApiResponse.success("Login successful", response));
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {

                // 1. Xóa session trong database nếu có
                userService.logout(refreshToken);

                // 2. Xóa Cookie "logged" ở trình duyệt (maxAge = 0)
                ResponseCookie cleanLoggedCookie = ResponseCookie.from("logged", "")
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("None")
                                .path("/")
                                .maxAge(0)
                                .build();

                // 3. Xóa Cookie "refreshToken" ở trình duyệt (maxAge = 0)
                ResponseCookie cleanRefreshCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("None")
                                .path("/")
                                .maxAge(0)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cleanLoggedCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
                                .body(ApiResponse.success("Logout successful", null));
        }

        @PostMapping("/refresh-token")
        public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
                        @CookieValue(name = "refreshToken", required = false) String cookieToken,
                        @RequestBody(required = false) RefreshTokenRequest body) {
                String refreshToken = cookieToken != null ? cookieToken : (body != null ? body.refreshToken() : null);
                if (refreshToken == null || refreshToken.isBlank()) {
                        throw new RuntimeException("Thiếu refresh token!");
                }
                String newAccessToken = userService.refreshToken(refreshToken);
                Map<String, String> data = Map.of("accessToken", newAccessToken);
                return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", data));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<UserResponse>> getMe(@RequestAttribute("userId") Long userId) {
                UserResponse request = userService.getMe(userId);
                return ResponseEntity.ok(ApiResponse.success("Lay du lieu thanh cong!", request));
        }

        @PostMapping("/send-otp")
        public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
                otpService.sendOtpByEmail(request.getEmail());
                return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi đến email của bạn", null));
        }

        @PostMapping("/verify-otp")
        public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
                        @RequestBody Map<String, Object> body) {
                Integer otp = ParseUtils.toInteger(body.get("otp"));
                String email = ParseUtils.toString(body.get("email"));

                boolean verified = otpService.verifyOtp(otp, email);

                if (verified) {
                        return ResponseEntity.ok(
                                        ApiResponse.success("Xác thực mã OTP thành công!"));
                }

                return ResponseEntity.badRequest().body(
                                ApiResponse.success("Mã OTP không chính xác hoặc đã hết hạn!"));
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<UserResponse>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {
                UserResponse response = userService.forgotPassword(request);
                return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", response));
        }
}
