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
        String systemPrompt = "You are a Senior DevOps Engineer. Generate a custom, production-ready, multi-stage Dockerfile.\n" +
                "STRICT RULES:\n" +
                "1. ARTIFACT NAME: The JAR file is named '" + analysis.getArtifactName() + "'. Use this specific name, NOT 'app.jar'.\n" +
                "2. HEALTHCHECK: " + (analysis.isHasActuator() ? "Use '/actuator/health' as the project HAS Spring Boot Actuator." : "DO NOT use '/actuator/health'. Use '" + (analysis.getHealthEndpoint() != null ? analysis.getHealthEndpoint() : "/") + "' or a TCP check instead.") + "\n" +
                "3. ENVIRONMENT: Include ENV variables for: " + (analysis.getDatabaseType() != null ? "Database (" + analysis.getDatabaseType() + "), " : "") + "Port (" + analysis.getPort() + ").\n" +
                "4. SECURITY: Use a non-root user (e.g., 'spring').\n" +
                "5. OPTIMIZATION: Multi-stage build is mandatory. Clean up cache.\n" +
                "6. .DOCKERIGNORE: Also suggest a small .dockerignore content at the end of the response inside a comment block.\n" +
                "Return ONLY the Dockerfile content, then the .dockerignore inside a comment block. No conversational text.\n\n";
        
        logger.info("Appel d'Ollama ({}) avec le modèle : {} pour le projet : {}", ollamaUrl, modelName, analysis.getArtifactId());
        
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
