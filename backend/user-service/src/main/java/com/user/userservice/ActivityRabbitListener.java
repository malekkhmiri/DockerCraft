package com.user.userservice;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Component
public class ActivityRabbitListener {

    private final ActivityService activityService;
    private final EmailService emailService;

    public ActivityRabbitListener(ActivityService activityService, EmailService emailService) {
        this.activityService = activityService;
        this.emailService = emailService;
    }

    public static final String ACTIVITY_LOG_QUEUE = "activity.log.queue";
    public static final String PROJECT_EXCHANGE = "project.exchange";
    
    public static final String PROJECT_UPLOADED_KEY = "project.uploaded.key";
    public static final String PIPELINE_FINISHED_KEY = "pipeline.finished.key";
    public static final String DOCKERFILE_READY_KEY = "dockerfile.ready.key";

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.routing.key";


    @RabbitListener(queues = ACTIVITY_LOG_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleEvent(Object event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        String type = "SYSTEM";
        String message = "Événement plateforme : " + event;
        
        switch (routingKey) {
            case PROJECT_UPLOADED_KEY:
                type = "UPLOAD";
                message = "Nouveau projet détecté (ID: " + event + ")";
                break;
            case PIPELINE_FINISHED_KEY:
                type = "PIPELINE";
                message = "Pipeline terminé avec succès pour le projet " + event;
                break;
            case DOCKERFILE_READY_KEY:
                type = "DOCKER";
                message = "Dockerfile généré par l'IA pour le projet " + event;
                break;
        }

        activityService.logActivity(type, message, "system");
    }

    @RabbitListener(queues = EMAIL_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleEmail(com.user.userservice.dto.EmailRequest request) {
        if ("VERIFICATION".equals(request.getType())) {
            emailService.sendVerificationCode(request.getTo(), request.getCode());
        } else {
            emailService.sendSimpleEmail(request.getTo(), request.getSubject(), request.getBody());
        }
    }
}
