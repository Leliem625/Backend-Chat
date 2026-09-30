package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    public CommandLineRunner checkDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                log.info("==================================================");
                log.info("✅ KẾT NỐI DATABASE THÀNH CÔNG!");
                log.info("Database Name: {}", connection.getCatalog());
                log.info("MySQL Version: {}", connection.getMetaData().getDatabaseProductVersion());
                log.info("Table 'users' đã được Hibernate tự động đồng bộ.");
                log.info("==================================================");
            } catch (Exception e) {
                log.error("❌ Không thể kết nối đến Database: {}", e.getMessage());
                log.error("Vui lòng kiểm tra lại DB_PASSWORD trong file .env!");
            }
        };
    }
}
