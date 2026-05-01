package com.user.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ACTIVITY_LOG_QUEUE = "activity.log.queue";
    public static final String PROJECT_EXCHANGE = "project.exchange";
    public static final String PROJECT_UPLOADED_KEY = "project.uploaded.key";
    public static final String PIPELINE_FINISHED_KEY = "pipeline.finished.key";
    public static final String DOCKERFILE_READY_KEY = "dockerfile.ready.key";

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.routing.key";

    // --- Queues ---
    @Bean
    public Queue activityQueue() {
        return new Queue(ACTIVITY_LOG_QUEUE, true);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    // --- Exchanges ---
    @Bean
    public DirectExchange projectExchange() {
        return new DirectExchange(PROJECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    // --- Bindings ---
    @Bean
    public Binding bindingUpload(Queue activityQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(activityQueue).to(projectExchange).with(PROJECT_UPLOADED_KEY);
    }

    @Bean
    public Binding bindingPipeline(Queue activityQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(activityQueue).to(projectExchange).with(PIPELINE_FINISHED_KEY);
    }

    @Bean
    public Binding bindingDockerfile(Queue activityQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(activityQueue).to(projectExchange).with(DOCKERFILE_READY_KEY);
    }

    @Bean
    public Binding bindingEmail(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
    }

    // --- JSON Serialization ---
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
