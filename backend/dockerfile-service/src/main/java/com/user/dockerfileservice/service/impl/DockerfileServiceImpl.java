package com.user.dockerfileservice.service.impl;

import com.user.dockerfileservice.entity.Dockerfile;
import com.user.dockerfileservice.repository.DockerfileRepository;
import com.user.dockerfileservice.service.DockerfileService;
import com.user.dockerfileservice.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.user.dockerfileservice.dto.ProjectResponse;
import com.user.dockerfileservice.dto.AnalysisResult;
import com.user.dockerfileservice.service.ProjectAnalysisService;
import com.user.dockerfileservice.service.ValidatorService;
import com.user.dockerfileservice.service.DockerfilePostProcessor;
import com.user.dockerfileservice.exception.UnsupportedLanguageException;
import com.user.dockerfileservice.strategy.LanguageStrategy;
import com.user.dockerfileservice.strategy.StrategyRegistry;
import com.user.dockerfileservice.service.DockerfileGenerationService;
import java.util.List;

@Service
public class DockerfileServiceImpl implements DockerfileService {

    private static final Logger logger = LoggerFactory.getLogger(DockerfileServiceImpl.class);
    private final DockerfileRepository repository;
    @org.springframework.context.annotation.Lazy
    private final LLMService llmService;
    private final ProjectAnalysisService analysisService;
    @org.springframework.context.annotation.Lazy
    private final ValidatorService validatorService;
    @org.springframework.context.annotation.Lazy
    private final DockerfilePostProcessor postProcessor;
    @org.springframework.context.annotation.Lazy
    private final DockerfileGenerationService intelligentGenerationService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public DockerfileServiceImpl(@org.springframework.context.annotation.Lazy DockerfileRepository repository,
            @org.springframework.context.annotation.Lazy LLMService llmService,
            @org.springframework.context.annotation.Lazy ProjectAnalysisService analysisService,
            @org.springframework.context.annotation.Lazy ValidatorService validatorService,
            @org.springframework.context.annotation.Lazy DockerfilePostProcessor postProcessor,
            @org.springframework.context.annotation.Lazy DockerfileGenerationService intelligentGenerationService,
            @org.springframework.beans.factory.annotation.Qualifier("externalRestTemplate") @org.springframework.context.annotation.Lazy org.springframework.web.client.RestTemplate restTemplate) {
        this.repository = repository;
        this.llmService = llmService;
        this.analysisService = analysisService;
        this.validatorService = validatorService;
        this.postProcessor = postProcessor;
        this.intelligentGenerationService = intelligentGenerationService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void generateDockerfile(Long projectId) {
        logger.info("🚀 Début de la génération intelligente pour le projet ID : {}", projectId);
        try {
            // 1. Récupérer les infos du projet (via hostname direct dc-project-service:8082)
            String projectUrl = "http://dc-project-service:8082/api/projects/" + projectId;
            ProjectResponse project = restTemplate.getForObject(projectUrl, ProjectResponse.class);
            
            if (project == null || project.getArchivePath() == null) {
                logger.error("❌ Projet ou chemin d'archive introuvable pour #{}", projectId);
                return;
            }

            // 2. Vérifier Quota (Désactivé pour l'unification Pro)
            // if (!checkAndConsumeQuota(project.getUserEmail())) return;
            logger.info("ℹ️ Quota ignoré (Mode illimité activé)");

            // 3. Analyse & Détection
            AnalysisResult analysis = analysisService.analyze(project.getArchivePath());
            
            // 4 & 5. Génération & Post-processing
            String content = generate(analysis);
            boolean isValid = validatorService.validate(content, analysis);

            // 6. Sauvegarde & Notification
            Dockerfile dockerfile = Dockerfile.builder()
                    .projectId(projectId)
                    .content(content)
                    .isValidated(isValid)
                    .build();
            
            repository.save(dockerfile);
            
            if (isValid) {
                logger.info("✅ Dockerfile généré et validé pour le projet #{}", projectId);
                // Mise à jour directe du statut du projet (Pipeline supprimé)
                updateProjectStatus(projectId, "SUCCESS");
            } else {
                logger.error("❌ Impossible de générer un Dockerfile valide après plusieurs tentatives.");
                updateProjectStatus(projectId, "FAILED");
            }

        } catch (Exception e) {
            logger.error("💥 Erreur critique lors de la génération", e);
        }
    }

    @Override
    public String generate(AnalysisResult analysis) {
        String generatedContent = intelligentGenerationService.generate(analysis);

        // Safety net: post-processing to guarantee Dockerfile correctness
        return postProcessor.process(generatedContent, analysis);
    }

    private void updateProjectStatus(Long projectId, String status) {
        try {
            String url = "http://dc-project-service:8082/api/projects/" + projectId + "/status?status=" + status;
            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du statut pour le projet #{}", projectId, e);
        }
    }

    @Override
    public Dockerfile getByProjectId(Long projectId) {
        return repository.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
    }

    @Override
    public Dockerfile getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<Dockerfile> getAll() {
        return repository.findAll();
    }

    @Override
    public Dockerfile updateDockerfile(Long id, String content) {
        // Au lieu d'écraser, on crée une nouvelle version pour l'historique
        Dockerfile old = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dockerfile source non trouvé"));
        
        Dockerfile newVersion = Dockerfile.builder()
                .projectId(old.getProjectId())
                .content(content)
                .build();
        
        return repository.save(newVersion);
    }

    @Override
    public List<Dockerfile> getHistoryByProjectId(Long projectId) {
        return repository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
