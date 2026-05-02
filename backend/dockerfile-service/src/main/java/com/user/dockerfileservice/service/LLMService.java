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
        String javaVer    = analysis.getJavaVersion() != null ? analysis.getJavaVersion() : "17";
        String framework  = analysis.getFramework();
        String dbType     = analysis.getDatabaseType();
        String artifact   = analysis.getArtifactName();
        String health     = analysis.getHealthEndpoint();
        
        boolean isPlain   = "java-plain".equalsIgnoreCase(framework);
        boolean hasDb     = dbType != null && !dbType.equalsIgnoreCase("h2") && !dbType.equalsIgnoreCase("none");
        boolean hasHealth = health != null && !health.isBlank();

        StringBuilder sb = new StringBuilder();

        // ── RÔLE ──────────────────────────────────────────────────────────────
        sb.append("""
            You are a professional Dockerfile generator. Your ONLY output is a valid, production-ready Dockerfile.
            No explanations. No markdown fences. No comments unless they are Dockerfile comments (#).
            Start directly with FROM.

            """);

        // ── CONTEXTE PROJET ───────────────────────────────────────────────────
        sb.append("## Project Facts\n");
        sb.append("- Framework : ").append(framework).append("\n");
        sb.append("- Java      : ").append(javaVer).append("\n");
        sb.append("- Database  : ").append(dbType != null ? dbType : "none").append("\n");
        sb.append("- JAR name  : ").append(artifact).append("\n");
        sb.append("- Health URL: ").append(hasHealth ? health : "none").append("\n\n");

        // ── CAS 1 : JAVA PLAIN (Console App) ──────────────────────────────────
        if (isPlain) {
            String jarBaseName = artifact.replace(".jar", "");
            sb.append("""
                ## CASE: java-plain (console application, NO web server)
                MANDATORY rules — violation = wrong output:
                - DO NOT add EXPOSE
                - DO NOT add HEALTHCHECK
                - DO NOT add ENV SPRING_* or DB variables
                - DO NOT add wget or any runtime package
                - DO NOT add a non-root user (unnecessary for a console app)
                - Use exec form ENTRYPOINT: ["java", "-jar", "app.jar"]
                - Multi-stage: maven:3.9.6-eclipse-temurin-%s-alpine → eclipse-temurin:%s-jre-alpine

                Expected output structure:
                FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
                WORKDIR /build
                COPY pom.xml .
                COPY src ./src
                RUN mvn clean package -B
                FROM eclipse-temurin:%s-jre-alpine AS runtime
                WORKDIR /app
                COPY --from=builder /build/target/%s app.jar
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """.formatted(javaVer, javaVer, javaVer, javaVer, artifact));
            return sb.toString();
        }

        // ── CAS 2 : SPRING BOOT ───────────────────────────────────────────────
        sb.append("""
            ## CASE: Spring Boot application
            MANDATORY build stage:
            - FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
            - WORKDIR /build
            - COPY pom.xml . then RUN mvn dependency:go-offline -B (cache layer)
            - COPY src ./src
            - RUN mvn clean package -DskipTests -B
            - DO NOT switch USER before the build — Maven needs write access

            MANDATORY runtime stage:
            - FROM eclipse-temurin:%s-jre-alpine AS runtime
            - Create non-root user BEFORE switching: RUN addgroup -S spring && adduser -S spring -G spring
            - USER spring:spring
            - WORKDIR /app
            - COPY --from=builder --chown=spring:spring /build/target/%s app.jar
            - ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
            """.formatted(javaVer, javaVer, artifact));

        // ── DB CONFIG ─────────────────────────────────────────────────────────
        if (hasDb) {
            String jdbcUrl = switch (dbType.toLowerCase()) {
                case "mysql"      -> "jdbc:mysql://db:3306/dbname";
                case "postgresql" -> "jdbc:postgresql://db:5432/dbname";
                default           -> "jdbc:h2:mem:testdb";
            };
            sb.append("""

                ## Database: %s
                Add these ENV vars (values injected at runtime):
                ENV SPRING_DATASOURCE_URL=%s \\
                    SPRING_DATASOURCE_USERNAME=CHANGE_ME_INJECT_AT_RUNTIME \\
                    SPRING_DATASOURCE_PASSWORD=CHANGE_ME_INJECT_AT_RUNTIME
                DO NOT install libpq if database is mysql.
                DO NOT install mysql-client if database is postgresql.
                """.formatted(dbType, jdbcUrl));
        } else {
            sb.append("""

                ## Database: none or H2 (embedded)
                DO NOT add SPRING_DATASOURCE_* ENV vars.
                DO NOT install any DB client package.
                """);
        }

        // ── HEALTHCHECK CONFIG ──────────────────────────────────────────────
        if (hasHealth) {
            sb.append("""

                ## Healthcheck
                ADD wget: apk add --no-cache wget (in the same RUN as addgroup)
                HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\
                  CMD wget --quiet --tries=1 --spider http://localhost:8080%s || exit 1
                """.formatted(health));
        } else {
            sb.append("""

                ## Healthcheck: none
                DO NOT add HEALTHCHECK.
                DO NOT install wget.
                """);
        }

        // ── ENV COMMUN ────────────────────────────────────────────────────────
        sb.append("""

            ## Always add these ENV vars for Spring Boot:
            ENV SPRING_PROFILES_ACTIVE=prod \\
                JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

            ## Always add:
            EXPOSE 8080
            """);

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
