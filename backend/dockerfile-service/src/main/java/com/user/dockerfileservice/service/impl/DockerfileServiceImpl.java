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
import com.user.dockerfileservice.util.DebugLogger;
import java.util.List;

@Service
public class DockerfileServiceImpl implements DockerfileService, DebugLogger {

    private static final Logger logger = LoggerFactory.getLogger(DockerfileServiceImpl.class);
    
    // Buffer de logs pour le debug (50 derniers messages)
    private static final List<String> debugLogs = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public static List<String> getDebugLogs() {
        return new java.util.ArrayList<>(debugLogs);
    }

    @Override
    public void log(String msg) {
        String logEntry = java.time.LocalDateTime.now() + " - " + msg;
        debugLogs.add(logEntry);
        logger.info(msg);
        if (debugLogs.size() > 50) debugLogs.remove(0);
    }

    private void addDebugLog(String msg) {
        log(msg);
    }
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

    private String getProjectServiceUrl() {
        String url = System.getenv("PROJECT_SERVICE_URL");
        if (url == null || url.isEmpty()) {
            return "https://dc-project-service-715286351060.us-central1.run.app";
        }
        return url;
    }

    @org.springframework.scheduling.annotation.Async
    @Override
    public void generateDockerfile(Long projectId) {
        addDebugLog("⚡ DÉMARRAGE ASYNCHRONE pour le projet #" + projectId);
        try {
            // 1. Récupérer les infos du projet
            String projectUrl = getProjectServiceUrl() + "/api/projects/" + projectId;
            addDebugLog("🔍 Récupération des infos projet : " + projectUrl);
            ProjectResponse project = restTemplate.getForObject(projectUrl, ProjectResponse.class);
            
            if (project == null || project.getArchivePath() == null) {
                addDebugLog("❌ Projet introuvable pour #" + projectId);
                return;
            }

            // 3. Analyse & Détection
            String downloadUrl = getProjectServiceUrl() + "/api/projects/" + projectId + "/download";
            addDebugLog("⬇️ Téléchargement du ZIP : " + downloadUrl);
            
            byte[] archiveData = restTemplate.getForObject(downloadUrl, byte[].class);
            if (archiveData == null) {
                addDebugLog("❌ Échec du téléchargement ZIP pour #" + projectId);
                return;
            }

            java.nio.file.Path tempZip = java.nio.file.Files.createTempFile("project-" + projectId + "-", ".zip");
            java.nio.file.Files.write(tempZip, archiveData);
            addDebugLog("📦 ZIP sauvegardé temporairement (Taille: " + archiveData.length + " bytes)");

            addDebugLog("🕵️‍♂️ Lancement de l'Analyse Statique...");
            AnalysisResult analysis = analysisService.analyze(tempZip.toString());
            addDebugLog("✅ Analyse terminée (Java: " + analysis.getJavaVersion() + ", Framework: " + analysis.getFramework() + ")");
            
            try { java.nio.file.Files.deleteIfExists(tempZip); } catch (Exception ignore) {}
            
            // 4 & 5. Génération & Post-processing
            addDebugLog("🤖 Appel à l'IA Ollama (Qwen)...");
            String content = generate(analysis);
            addDebugLog("✅ Réponse IA reçue (Taille: " + content.length() + " chars)");
            
            boolean isValid = validatorService.validate(content, analysis);
            addDebugLog("⚖️ Validation Post-Processing : " + (isValid ? "VALIDE" : "FALLBACK"));
            
            String method = "AI";
            if (content.startsWith("# METHOD: FALLBACK")) {
                method = "FALLBACK";
            }

            // 6. Sauvegarde
            try {
                Dockerfile dockerfile = Dockerfile.builder()
                        .projectId(projectId)
                        .content(content)
                        .isValidated(isValid)
                        .generationMethod(method)
                        .build();
                
                repository.save(dockerfile);
                addDebugLog("💾 Dockerfile sauvegardé en DB !");
            } catch (Exception e) {
                addDebugLog("❌ Erreur DB : " + e.getMessage());
            }
            
            if (isValid) {
                logger.info("✅ Dockerfile généré et validé pour le projet #{}", projectId);
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
        // ON BYPASSE le service de template intelligent qui causait des conflits de prompts
        // On va directement vers l'IA avec les faits du projet.
        String prompt = "Analyze this project: Language=" + analysis.getLanguage() + 
                        ", BuildTool=" + analysis.getBuildTool() + 
                        ", Framework=" + analysis.getFramework();
        String generatedContent = llmService.generate(analysis, prompt);

        // Safety net: post-processing to guarantee Dockerfile correctness
        return postProcessor.process(generatedContent, analysis);
    }

    private void updateProjectStatus(Long projectId, String status) {
        try {
            String url = getProjectServiceUrl() + "/api/projects/" + projectId + "/status?status=" + status;
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
