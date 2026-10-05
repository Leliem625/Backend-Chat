package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    // @NotBlank(message = "Mã OTP không được để trống")
    // @Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 ký tự")
    // private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String passwordNew;

    public ForgotPasswordRequest() {
    }

    public ForgotPasswordRequest(String email, String passwordNew) {
        this.email = email;
        // this.otp = otp;
        this.passwordNew = passwordNew;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // public String getOtp() {
    // return otp;
    // }

    // public void setOtp(String otp) {
    // this.otp = otp;
    // }

    public String getPasswordNew() {
        return passwordNew;
    }

    public void setPasswordNew(String passwordNew) {
        this.passwordNew = passwordNew;
    }
}
