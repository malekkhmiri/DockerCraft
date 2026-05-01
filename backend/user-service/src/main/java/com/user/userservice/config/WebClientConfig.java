package com.user.userservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final String INTERNAL_SECRET_VALUE = "my-super-secret-key-12345";

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        // Apply the X-Internal-Secret header directly here so it's included
        // in ALL inter-service WebClient calls, including load-balanced ones.
        return WebClient.builder()
                .defaultHeader("X-Internal-Secret", INTERNAL_SECRET_VALUE);
    }
}
