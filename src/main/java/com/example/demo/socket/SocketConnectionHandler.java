package com.example.demo.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOServer;

@Component
public class SocketConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);

    public SocketConnectionHandler(SocketIOServer server) {
        server.addConnectListener(client -> {
            Long userId = client.get("userId");
            client.joinRoom("user:" + userId);
            log.info("Socket kết nối: userId = {}", userId);
        });

        server.addDisconnectListener(client -> {
            Long userId = client.get("userId");
            log.info("Socket ngắt kết nối: userId = {}", userId);
        });
    }
}