package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setTo(toEmail);
            message.setSubject("Mã OTP xác thực đặt lại mật khẩu");
            message.setText("Xin chào,\n\n"
                    + "Mã OTP của bạn để đặt lại mật khẩu là: " + otp + "\n\n"
                    + "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n"
                    + "Trân trọng,\nĐội ngũ hỗ trợ.");

            mailSender.send(message);
            log.info("Đã gửi mã OTP thành công tới email: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP tới {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP: " + e.getMessage(), e);
        }
    }
}
