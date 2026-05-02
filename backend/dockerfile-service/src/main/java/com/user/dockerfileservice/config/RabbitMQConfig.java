package com.user.dockerfileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RabbitMQConfig {
    // Classe vidée pour le déploiement Cloud Run (Option Nucléaire)
    
    @Bean(name = "externalRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
