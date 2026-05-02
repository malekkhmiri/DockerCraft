package com.user.projectservice.service.impl;

import com.user.projectservice.config.RabbitMQConfig;
import com.user.projectservice.dto.ProjectResponse;
import com.user.projectservice.entity.Project;
import com.user.projectservice.entity.ProjectLanguage;
import com.user.projectservice.entity.ProjectStatus;
import com.user.projectservice.repository.ProjectRepository;
import com.user.projectservice.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ProjectRepository projectRepository;
    // private final RabbitTemplate rabbitTemplate;

    @Value("${project.upload.dir:/tmp/uploads}")
    private String uploadDir;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public ProjectResponse uploadProject(String name, String language, String userEmail, String username, MultipartFile file) {
        try {
            logger.info("Début de l'upload du projet : {} pour l'utilisateur : {}", name, userEmail);
            
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = root.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            ProjectLanguage projectLanguage = parseLanguage(language);

            Project project = Project.builder()
                    .name(name)
                    .language(projectLanguage)
                    .userEmail(userEmail)
                    .username(username)
                    .archivePath(filePath.toString())
                    .reference("REF-" + (System.currentTimeMillis() % 1000000))
                    .executionTime("En attente")
                    .status(ProjectStatus.UPLOADED)
                    .build();

            Project savedProject = projectRepository.save(project);
            logger.info("Projet enregistré en base avec l'ID : {}", savedProject.getId());

            // Appel synchrone au Dockerfile Service au lieu de RabbitMQ
            try {
                String dockerfileServiceUrl = System.getenv("DOCKERFILE_SERVICE_URL");
                if (dockerfileServiceUrl == null) {
                    dockerfileServiceUrl = "https://dc-dockerfile-service-715286351060.us-central1.run.app";
                }
                String generateUrl = dockerfileServiceUrl + "/api/dockerfiles/project/" + savedProject.getId() + "/generate";
                logger.info("Appel du service de génération : {}", generateUrl);
                
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                restTemplate.postForEntity(generateUrl, null, Void.class);
                logger.info("Demande de génération envoyée avec succès.");
            } catch (Exception e) {
                logger.error("Erreur lors de l'appel au service de génération : {}", e.getMessage());
                // On ne bloque pas l'upload si la génération échoue
            }

            return mapToResponse(savedProject);
        } catch (IOException e) {
            throw new RuntimeException("Échec du stockage du fichier : " + e.getMessage());
        }
    }

    private ProjectLanguage parseLanguage(String language) {
        if (language == null || "Générique".equalsIgnoreCase(language)) return ProjectLanguage.UNKNOWN;
        
        String lang = language.toUpperCase().trim()
                .replace(".", "")
                .replace("-", "")
                .replace(" ", "");
        
        if (lang.contains("JAVA")) return ProjectLanguage.JAVA;
        if (lang.contains("NODE")) return ProjectLanguage.NODEJS;
        if (lang.contains("PYTHON")) return ProjectLanguage.PYTHON;
        if (lang.contains("GO")) return ProjectLanguage.GO;
        if (lang.contains("PHP")) return ProjectLanguage.PHP;
        
        try {
            return ProjectLanguage.valueOf(lang);
        } catch (IllegalArgumentException e) {
            return ProjectLanguage.UNKNOWN;
        }
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponse getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
    }

    @Override
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    @Override
    public long countProjects() {
        return projectRepository.count();
    }

    @Override
    public List<ProjectResponse> getProjectsByUserEmail(String userEmail) {
        return projectRepository.findByUserEmail(userEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countProjectsByUserEmail(String userEmail) {
        return projectRepository.countByUserEmail(userEmail);
    }

    @Override
    public void updateStatus(Long id, String status) {
        projectRepository.findById(id).ifPresent(project -> {
            project.setStatus(ProjectStatus.valueOf(status.toUpperCase()));
            projectRepository.save(project);
            logger.info("Statut du projet #{} mis à jour vers {}", id, status);
        });
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .createdAt(project.getCreatedAt())
                .status(project.getStatus() != null ? project.getStatus() : ProjectStatus.UPLOADED)
                .language(project.getLanguage())
                .userEmail(project.getUserEmail())
                .username(project.getUsername())
                .archivePath(project.getArchivePath())
                .extractedPath(project.getExtractedPath())
                .executionTime(project.getExecutionTime() != null ? project.getExecutionTime() : "0s")
                .reference(project.getReference() != null ? project.getReference() : "PROJ-" + project.getId())
                .build();
    }
}
