package com.example.demo.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.example.demo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class SocketConfig {

    @Value("${socket.port:9092}")
    private int port;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketIOServer socketIOServer(JwtService jwtService) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0"); // Cho phép điện thoại trong mạng LAN kết nối
        config.setPort(port);

        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()) {
            @Override
            protected void init(ObjectMapper objectMapper) {
                super.init(objectMapper);
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        });

        // Xác thực lúc bắt tay:
        // 1. Mobile (React Native): gửi qua header "Authorization: Bearer <token>"
        // 2. Web Browser: WebSocket trên trình duyệt không cho phép gửi custom header,
        //    nên gửi qua query param "?token=<token>" hoặc "?accessToken=<token>"
        config.setAuthorizationListener(handshakeData -> {
            String token = null;

            // 1. Kiểm tra trong Header (dành cho Mobile)
            String header = handshakeData.getHttpHeaders().get("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }

            // 2. Nếu không có ở Header (dành cho Web Browser), lấy từ Query Param
            if (token == null || token.isBlank()) {
                token = handshakeData.getSingleUrlParam("token");
            }
            if (token == null || token.isBlank()) {
                token = handshakeData.getSingleUrlParam("accessToken");
            }

            if (token == null || token.isBlank()) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            try {
                Long userId = jwtService.extractUserId(token);
                // userId được lưu vào client, lấy lại bằng client.get("userId")
                return new AuthorizationResult(true, Map.of("userId", userId));
            } catch (Exception e) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        return new SocketIOServer(config);
    }
}