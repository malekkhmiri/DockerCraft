package com.user.dockerfileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.user.dockerfileservice", "com.platform.dockerfileservice"})
@EntityScan(basePackages = {"com.user.dockerfileservice.entity", "com.platform.dockerfileservice.model"})
@EnableJpaRepositories(basePackages = {"com.user.dockerfileservice.repository", "com.platform.dockerfileservice.repository"})
public class DockerfileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DockerfileServiceApplication.class, args);
    }
}
