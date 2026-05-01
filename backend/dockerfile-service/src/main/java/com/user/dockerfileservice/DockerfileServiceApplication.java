package com.user.dockerfileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.user", "com.platform"})
@EnableDiscoveryClient
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {"com.user.dockerfileservice.repository", "com.platform.dockerfileservice.repository"})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {"com.user.dockerfileservice", "com.platform.dockerfileservice"})
public class DockerfileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DockerfileServiceApplication.class, args);
    }
}
