package com.user.projectservice.config;

// import org.springframework.amqp.core.*;
// import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
// import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration
public class RabbitMQConfig {

    // Upload project (DirectExchange)
    public static final String PROJECT_UPLOADED_QUEUE = "project.uploaded.queue";
    public static final String PROJECT_EXCHANGE = "project.exchange";
    public static final String PROJECT_UPLOADED_ROUTING_KEY = "project.uploaded.key";

    // Pipeline finished - chaque service a SA PROPRE file (FanoutExchange)
    public static final String PIPELINE_FINISHED_PROJECT_QUEUE = "pipeline.finished.project.queue";
    public static final String PIPELINE_FINISHED_EXCHANGE = "pipeline.finished.fanout.exchange";

    /*
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    */

    /*
    @Bean
    public Queue projectUploadedQueue() {
        return new Queue(PROJECT_UPLOADED_QUEUE);
    }
    */

    /*
    @Bean
    public Queue pipelineFinishedProjectQueue() {
        return new Queue(PIPELINE_FINISHED_PROJECT_QUEUE);
    }
    */

    /*
    @Bean
    public DirectExchange projectExchange() {
        return new DirectExchange(PROJECT_EXCHANGE);
    }
    */

    /*
    @Bean
    public FanoutExchange pipelineFinishedExchange() {
        return new FanoutExchange(PIPELINE_FINISHED_EXCHANGE);
    }
    */

    /*
    @Bean
    public Binding projectUploadBinding(Queue projectUploadedQueue, DirectExchange projectExchange) {
        return BindingBuilder.bind(projectUploadedQueue).to(projectExchange).with(PROJECT_UPLOADED_ROUTING_KEY);
    }
    */

    /*
    @Bean
    public Binding pipelineFinishedProjectBinding(Queue pipelineFinishedProjectQueue, FanoutExchange pipelineFinishedExchange) {
        return BindingBuilder.bind(pipelineFinishedProjectQueue).to(pipelineFinishedExchange);
    }
    */
}
