package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@org.springframework.context.annotation.Lazy
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final RestTemplate restTemplate;

    @Value("${OLLAMA_URL:http://dc-ollama:8080}")
    private String ollamaUrl;

    @Value("${OLLAMA_MODEL:qwen2.5-coder:3b}")
    private String modelName;

    public LLMService(@org.springframework.beans.factory.annotation.Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generate(AnalysisResult analysis, String prompt) {
        String dbType = analysis.getDatabaseType() != null ? analysis.getDatabaseType() : "H2 (no external DB)";
        String javaVer = analysis.getJavaVersion();
        
        String systemPrompt = "You are an Elite DevOps Engineer. Task: Generate a CUSTOM Dockerfile for this SPECIFIC project.\n" +
                "FACTS FOR THIS PROJECT (DO NOT HALLUCINATE):\n" +
                "- TECHNOLOGY: Java " + javaVer + " (use eclipse-temurin:" + javaVer + " as base)\n" +
                "- DATABASE: " + dbType + " (DO NOT add PostgreSQL/libpq if using MySQL)\n" +
                "- ARTIFACT: " + analysis.getArtifactName() + "\n" +
                "- HEALTHCHECK: " + (analysis.isHasActuator() ? "/actuator/health" : (analysis.getHealthEndpoint() != null ? analysis.getHealthEndpoint() : "/")) + "\n\n" +
                "STRICT INSTRUCTIONS:\n" +
                "1. If project uses MySQL, DO NOT install libpq or postgresql-client.\n" +
                "2. Match the Java version EXACTLY (" + javaVer + ").\n" +
                "3. Use multi-stage build (maven:3.9-eclipse-temurin-" + javaVer + " as builder).\n" +
                "4. Use a non-root user 'devops'.\n" +
                "5. Ignore previous examples if they use different Java versions or DB types.\n" +
                "Return ONLY the Dockerfile code, then the .dockerignore inside a comment.\n";
        
        logger.info("🚀 Génération ELITE pour {} (Java {}, DB {})", analysis.getArtifactId(), javaVer, dbType);
        
        Map<String, Object> request = Map.of(
            "model", modelName,
            "prompt", systemPrompt + prompt,
            "stream", false
        );

        try {
            OllamaResponse response = restTemplate.postForObject(ollamaUrl + "/api/generate", request, OllamaResponse.class);
            if (response != null && response.getResponse() != null) {
                return cleanResponse(response.getResponse());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à Ollama", e);
        }

        return "# Erreur lors de la génération par l'IA\nFROM alpine\nCMD [\"echo\", \"error\"]";
    }

    private String cleanResponse(String response) {
        // 1. Enlever les balises markdown
        String cleaned = response.replaceAll("(?s)```(?:dockerfile)?(.*?)```", "$1");
        
        // 2. Extraire uniquement du premier FROM jusqu'à la fin de la dernière instruction Docker probable
        // (Cela évite les bavardages de l'IA à la fin)
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?s)(FROM\\s+.*)").matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group(1);
        }

        return cleaned.trim();
    }

    @Data
    private static class OllamaResponse {
        private String response;
    }
}
