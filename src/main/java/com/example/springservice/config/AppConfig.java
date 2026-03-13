package com.example.springservice.config;

import com.example.springservice.security.AuthProperties;
import com.example.springservice.security.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AppProperties.class, JwtProperties.class, AuthProperties.class})
public class AppConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    HttpClient httpClient(AppProperties appProperties) {
        return HttpClient.newBuilder()
            .connectTimeout(appProperties.getHuggingFace().getConnectTimeout())
            .build();
    }
}
