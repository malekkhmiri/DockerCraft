package com.user.userservice.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            // 5 minutes de timeout pour laisser l'IA charger et traiter l'image
            factory.setConnectTimeout((int) Duration.ofMinutes(5).toMillis());
            factory.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
            restClientBuilder.requestFactory(factory);
        };
    }
}
