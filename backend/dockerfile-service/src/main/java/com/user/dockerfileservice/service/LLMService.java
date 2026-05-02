package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final RestTemplate restTemplate;

    private static final String ERROR_DOCKERFILE = """
            # Erreur lors de la génération
            FROM alpine
            CMD ["echo", "error"]
            """;

    @Value("${OLLAMA_MODEL:qwen2.5-coder:3b}")
    private String modelName;

    @Value("${OLLAMA_URL:http://dc-ollama:8080}")
    private String ollamaUrl;

    public LLMService(@Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generate(AnalysisResult analysis, String userPrompt) {
        String systemPrompt = buildSystemPrompt(analysis);
        logger.info("🚀 Génération ARCHITECTE pour {} (Java {}, DB {})",
                analysis.getArtifactId(),
                analysis.getJavaVersion(),
                analysis.getDatabaseType());
        return callOllama(systemPrompt, userPrompt);
    }

    private String buildSystemPrompt(AnalysisResult analysis) {
        String dbType = analysis.getDatabaseType() != null ? analysis.getDatabaseType() : "H2";
        String javaVer = analysis.getJavaVersion() != null ? analysis.getJavaVersion() : "17";
        String port = String.valueOf(analysis.getPort() > 0 ? analysis.getPort() : 8080);

        StringBuilder sb = new StringBuilder();
        sb.append("You are an Expert AI & DevOps Architect. Task: Generate a production-grade, hyper-personalized Dockerfile.\n");
        sb.append("PROJECT CONTEXT:\n");
        sb.append("- Java Version: ").append(javaVer).append("\n");
        sb.append("- Framework: ").append(analysis.getFramework()).append("\n");
        sb.append("- Build Tool: ").append(analysis.getBuildTool()).append("\n");
        sb.append("- Artifact: ").append(analysis.getArtifactName()).append("\n");
        sb.append("- Service Port: ").append(port).append("\n");
        sb.append("- Database Driver: ").append(dbType).append("\n");
        
        if (analysis.isHasSecurity()) sb.append("- SECURITY: Spring Security detected. Ensure proper ENV for credentials.\n");
        if (analysis.isHasLombok()) sb.append("- LOMBOK: Used in project.\n");
        if (analysis.getHealthEndpoint() != null) sb.append("- HEALTHCHECK: Endpoint is ").append(analysis.getHealthEndpoint()).append("\n");

        sb.append("\nSTRICT ARCHITECTURAL RULES:\n");
        sb.append("1. MULTI-STAGE: Build in 'maven:3.9.6-eclipse-temurin-").append(javaVer).append("-alpine' as 'builder'.\n");
        sb.append("2. RUNTIME: Use 'eclipse-temurin:").append(javaVer).append("-jre-alpine' as 'runtime'.\n");
        sb.append("3. SECURITY: Run as non-root user 'appuser'.\n");
        sb.append("4. OPTIMIZATION: Use -XX:+UseContainerSupport and -XX:MaxRAMPercentage=75.0.\n");
        sb.append("5. DYNAMIC ENV: If DB is MySQL/Postgres, use ENV variables for DB_URL, DB_USER, DB_PASS.\n");
        if (analysis.getHealthEndpoint() != null) {
            sb.append("6. HEALTHCHECK: Use 'wget' or 'curl' targeting http://localhost:").append(port).append(analysis.getHealthEndpoint()).append(".\n");
        }
        sb.append("7. ENTRYPOINT: Must use ENTRYPOINT [\"sh\", \"-c\", \"exec java $JAVA_OPTS -jar app.jar\"].\n");
        sb.append("\nReturn ONLY the Dockerfile code.");

        return sb.toString();
    }

    private String callOllama(String systemPrompt, String prompt) {
        try {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("model", modelName);
            request.put("system", systemPrompt);
            request.put("prompt", prompt != null ? prompt : "Generate a production-ready Dockerfile");
            request.put("stream", false);

            com.user.dockerfileservice.service.impl.DockerfileServiceImpl.addDebugLogStatic("📡 Envoi de la requête à Ollama (" + ollamaUrl + ")...");
            
            OllamaResponse response = restTemplate.postForObject(
                    ollamaUrl + "/api/generate", request, OllamaResponse.class);
            if (response != null && response.response() != null) {
                String result = cleanResponse(response.response());
                com.user.dockerfileservice.service.impl.DockerfileServiceImpl.addDebugLogStatic("✨ Réponse brute reçue de Ollama (" + result.length() + " chars)");
                return result;
            }
        } catch (Exception e) {
            com.user.dockerfileservice.service.impl.DockerfileServiceImpl.addDebugLogStatic("❌ ERREUR OLLAMA: " + e.getMessage());
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
