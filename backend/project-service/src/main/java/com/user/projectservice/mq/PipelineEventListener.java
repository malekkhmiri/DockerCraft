package com.user.projectservice.mq;

import com.user.projectservice.config.RabbitMQConfig;
import com.user.projectservice.dto.PipelineFinishedEvent;
import com.user.projectservice.entity.Project;
import com.user.projectservice.entity.ProjectStatus;
import com.user.projectservice.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PipelineEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEventListener.class);
    private final ProjectRepository projectRepository;

    public PipelineEventListener(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // @RabbitListener(queues = RabbitMQConfig.PIPELINE_FINISHED_PROJECT_QUEUE)
    public void handlePipelineFinished(PipelineFinishedEvent event) {
        logger.info("Réception de la fin du pipeline pour le projet ID : {}, Statut : {}, Durée : {}", 
                event.getProjectId(), event.getStatus(), event.getExecutionTime());
        
        projectRepository.findById(event.getProjectId()).ifPresent(project -> {
            project.setStatus(ProjectStatus.valueOf(event.getStatus()));
            project.setExecutionTime(event.getExecutionTime());
            projectRepository.save(project);
            logger.info("Projet ID : {} mis à jour avec le temps d'exécution : {}", project.getId(), event.getExecutionTime());
        });
    }
}
