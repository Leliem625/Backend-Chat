package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Bật @Scheduled cho các cron job (đồng bộ user sang Elasticsearch...)
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
