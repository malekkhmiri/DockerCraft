package com.user.dockerfileservice.mq;

import com.user.dockerfileservice.config.RabbitMQConfig;
import com.user.dockerfileservice.service.DockerfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.user.dockerfileservice.dto.ProjectUploadedEvent;

@Component
public class ProjectEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProjectEventListener.class);
    private final DockerfileService dockerfileService;

    public ProjectEventListener(DockerfileService dockerfileService) {
        this.dockerfileService = dockerfileService;
    }

    @RabbitListener(queues = RabbitMQConfig.PROJECT_UPLOADED_QUEUE)
    public void handleProjectUploaded(ProjectUploadedEvent event) {
        logger.info("Evénement PROJECT_UPLOADED reçu pour le projet ID : {}", event.getProjectId());
        try {
            dockerfileService.generateDockerfile(event.getProjectId());
        } catch (Exception e) {
            logger.error("Erreur lors de la gestion de l'événement pour le projet {}", event.getProjectId(), e);
        }
    }
}
