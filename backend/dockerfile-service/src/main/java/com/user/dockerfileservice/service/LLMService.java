package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@org.springframework.context.annotation.Lazy
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private static final String ERROR_DOCKERFILE = """
            # Erreur lors de la génération
            FROM alpine
            CMD ["echo", "error"]
            """;

    private final RestTemplate restTemplate;

    @Value("${OLLAMA_URL:http://dc-ollama:8080}")
    private String ollamaUrl;

    @Value("${OLLAMA_MODEL:qwen2.5-coder:3b}")
    private String modelName;

    public LLMService(@Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generate(AnalysisResult analysis, String userPrompt) {
        String systemPrompt = buildSystemPrompt(analysis);
        logger.info("🚀 Génération ELITE pour {} (Java {}, DB {})",
                analysis.getArtifactId(),
                analysis.getJavaVersion(),
                analysis.getDatabaseType());
        return callOllama(systemPrompt, userPrompt);
    }

    private String buildSystemPrompt(AnalysisResult analysis) {
        String dbType = analysis.getDatabaseType() != null
                ? analysis.getDatabaseType()
                : "H2 (no external DB)";
        String javaVer = analysis.getJavaVersion();
        String healthEndpoint = analysis.getHealthEndpoint() != null
                ? analysis.getHealthEndpoint()
                : "NONE (DO NOT ADD ONE IF NONE)";

        return """
                You are an Elite DevOps Engineer. Task: Generate a CUSTOM Dockerfile for this SPECIFIC project.
                FACTS FOR THIS PROJECT (DO NOT HALLUCINATE):
                - TECHNOLOGY: Java %s (use eclipse-temurin:%s as base)
                - DATABASE: %s (DO NOT add PostgreSQL/libpq if using MySQL)
                - ARTIFACT: %s
                - HEALTHCHECK: %s

                STRICT INSTRUCTIONS:
                1. BUILDER IMAGE: Use EXACTLY 'maven:3.9.6-eclipse-temurin-%s-alpine' as the first stage 'builder'.
                2. RUNTIME IMAGE: Use EXACTLY 'eclipse-temurin:%s-jre-alpine' as the second stage 'runtime'.
                3. DIRECTORIES: Use '/build' for builder and '/app' for runtime.
                4. Match the Java version EXACTLY (%s).
                5. Use 'exec' in ENTRYPOINT: ENTRYPOINT ["sh", "-c", "exec java ..."].
                Return ONLY the Dockerfile code, no explanation.
                """.formatted(javaVer, javaVer, dbType, analysis.getArtifactName(),
                        healthEndpoint, javaVer);
    }

    private String callOllama(String systemPrompt, String prompt) {
        Map<String, Object> request = Map.of(
                "model", modelName,
                "system", systemPrompt,
                "prompt", prompt,
                "stream", false
        );
        try {
            OllamaResponse response = restTemplate.postForObject(
                    ollamaUrl + "/api/generate", request, OllamaResponse.class);
            if (response != null && response.response() != null) {
                return cleanResponse(response.response());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à Ollama", e);
        }
        return ERROR_DOCKERFILE;
    }

    private String cleanResponse(String response) {
        String cleaned = response.replaceAll("(?s)```(?:dockerfile|docker)?(.*?)```", "$1");
        Matcher matcher = Pattern.compile("(?s)(FROM\\s+.+)").matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group(1)
                    .replaceAll("(?m)^(?!FROM|RUN|COPY|ADD|ENV|EXPOSE|HEALTHCHECK|ENTRYPOINT|CMD|WORKDIR|ARG|LABEL|USER|VOLUME|#).*$\\n?", "");
        }
        return cleaned.trim();
    }

    private record OllamaResponse(String response) {}
}
