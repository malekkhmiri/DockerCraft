package com.user.dockerfileservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    public static final String PROJECT_UPLOADED_QUEUE = "project.uploaded.queue";
    public static final String DOCKERFILE_READY_QUEUE = "dockerfile.ready.queue";
    public static final String PROJECT_EXCHANGE = "project.exchange";
    public static final String PROJECT_UPLOADED_ROUTING_KEY = "project.uploaded.key";
    public static final String DOCKERFILE_READY_ROUTING_KEY = "dockerfile.ready.key";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue projectUploadedQueue() {
        return new Queue(PROJECT_UPLOADED_QUEUE);
    }

    @Bean
    public Queue dockerfileReadyQueue() {
        return new Queue(DOCKERFILE_READY_QUEUE);
    }

    @Bean
    public DirectExchange projectExchange() {
        return new DirectExchange(PROJECT_EXCHANGE);
    }

    @Bean
    public Binding projectUploadBinding(Queue projectUploadedQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(projectUploadedQueue).to(projectExchange).with(PROJECT_UPLOADED_ROUTING_KEY);
    }

    @Bean
    public Binding dockerfileBinding(Queue dockerfileReadyQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(dockerfileReadyQueue).to(projectExchange).with(DOCKERFILE_READY_ROUTING_KEY);
    }
}
