package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import com.user.dockerfileservice.util.DebugLogger;
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

    private static final String USER_TURN =
            "Generate the Dockerfile for this project now. Output only the Dockerfile, nothing else.";

    private static final String ERROR_DOCKERFILE = """
            # Erreur lors de la génération
            FROM alpine
            CMD ["echo", "error"]
            """;

    private final RestTemplate restTemplate;
    private final DebugLogger debugLogger;

    @Value("${OLLAMA_MODEL:qwen2.5-coder:3b}")
    private String modelName;

    @Value("${OLLAMA_URL:http://dc-ollama:8080}")
    private String ollamaUrl;

    public LLMService(@Qualifier("externalRestTemplate") RestTemplate restTemplate,
                      DebugLogger debugLogger) {
        this.restTemplate = restTemplate;
        this.debugLogger  = debugLogger;
    }

    public String generate(AnalysisResult analysis) {
        String systemPrompt = buildSystemPrompt(analysis);
        logger.info("Génération pour {} (Java {}, DB {})",
                analysis.getArtifactId(),
                analysis.getJavaVersion(),
                analysis.getDatabaseType());
        return callOllama(systemPrompt);
    }

    private String buildSystemPrompt(AnalysisResult analysis) {
        String javaVer   = analysis.getJavaVersion()   != null ? analysis.getJavaVersion()   : "17";
        String framework = analysis.getFramework()     != null ? analysis.getFramework()     : "spring-boot";
        String dbType    = analysis.getDatabaseType();
        String artifact  = analysis.getArtifactName()  != null ? analysis.getArtifactName()  : "app.jar";
        String health    = analysis.getHealthEndpoint();

        boolean isPlain   = "java-plain".equalsIgnoreCase(framework);
        boolean hasDb     = dbType != null
                         && !dbType.equalsIgnoreCase("h2")
                         && !dbType.equalsIgnoreCase("none");
        boolean hasHealth = health != null && !health.isBlank();

        StringBuilder sb = new StringBuilder();

        sb.append("""
            You are a Dockerfile generator. Your ONLY output is a valid Dockerfile.
            No explanations. No markdown. Start directly with FROM.

            """);

        sb.append("## Project Facts\n")
          .append("- Framework : ").append(framework).append("\n")
          .append("- Java      : ").append(javaVer).append("\n")
          .append("- Database  : ").append(dbType != null ? dbType : "none").append("\n")
          .append("- JAR name  : ").append(artifact).append("\n")
          .append("- Health URL: ").append(hasHealth ? health : "none").append("\n\n");

        if (isPlain) {
            sb.append("""
                ## CASE: java-plain (console app — NO web server)
                STRICT rules:
                - DO NOT add EXPOSE, HEALTHCHECK, ENV SPRING_*, wget, or non-root user
                - ENTRYPOINT must be exec form: ["java", "-jar", "app.jar"]

                Generate exactly this structure:
                FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
                WORKDIR /build
                COPY pom.xml .
                COPY src ./src
                RUN mvn clean package -B
                FROM eclipse-temurin:%s-jre-alpine AS runtime
                WORKDIR /app
                COPY --from=builder /build/target/%s app.jar
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """.formatted(javaVer, javaVer, artifact));
            return sb.toString();
        }

        sb.append("""
            ## CASE: Spring Boot
            Build stage (DO NOT switch USER here — Maven needs write access):
            - FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
            - WORKDIR /build
            - COPY pom.xml . && RUN mvn dependency:go-offline -B
            - COPY src ./src
            - RUN mvn clean package -DskipTests -B

            Runtime stage:
            - FROM eclipse-temurin:%s-jre-alpine AS runtime
            - RUN addgroup -S spring && adduser -S spring -G spring%s
            - USER spring:spring
            - WORKDIR /app
            - ENV SPRING_PROFILES_ACTIVE=prod JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            - COPY --from=builder --chown=spring:spring /build/target/%s app.jar
            - EXPOSE 8080
            - ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
            """.formatted(
                javaVer, javaVer,
                hasHealth ? " \\\\\n      && apk add --no-cache wget" : "",
                artifact));

        if (hasDb) {
            String jdbcUrl = switch (dbType.toLowerCase()) {
                case "mysql"      -> "jdbc:mysql://db:3306/dbname";
                case "postgresql" -> "jdbc:postgresql://db:5432/dbname";
                default           -> "jdbc:h2:mem:testdb";
            };
            sb.append("""

                ## Database: %s — add exactly:
                ENV SPRING_DATASOURCE_URL=%s \\
                    SPRING_DATASOURCE_USERNAME=CHANGE_ME_INJECT_AT_RUNTIME \\
                    SPRING_DATASOURCE_PASSWORD=CHANGE_ME_INJECT_AT_RUNTIME
                %s
                """.formatted(
                    dbType, jdbcUrl,
                    dbType.equalsIgnoreCase("mysql")
                        ? "DO NOT install libpq or postgresql-client."
                        : "DO NOT install mysql-client."));
        } else {
            sb.append("\n## No external DB — DO NOT add SPRING_DATASOURCE_* or any DB client.\n");
        }

        if (hasHealth) {
            sb.append("""

                ## Healthcheck — add exactly:
                HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\
                  CMD wget --quiet --tries=1 --spider http://localhost:8080%s || exit 1
                """.formatted(health));
        } else {
            sb.append("\n## No healthcheck — DO NOT add HEALTHCHECK or wget.\n");
        }

        return sb.toString();
    }

    private String callOllama(String systemPrompt) {
        debugLogger.log("Envoi requête à Ollama (" + ollamaUrl + ")");
        try {
            Map<String, Object> request = Map.of(
                    "model",  modelName,
                    "system", systemPrompt,
                    "prompt", USER_TURN,
                    "stream", false
            );
            OllamaResponse response = restTemplate.postForObject(
                    ollamaUrl + "/api/generate", request, OllamaResponse.class);
            if (response != null && response.response() != null) {
                String result = cleanResponse(response.response());
                debugLogger.log("Réponse reçue (" + result.length() + " chars)");
                return result;
            }
        } catch (Exception e) {
            debugLogger.log("ERREUR OLLAMA: " + e.getMessage());
            logger.error("Erreur lors de l'appel à Ollama", e);
        }
        return ERROR_DOCKERFILE;
    }

    private String cleanResponse(String response) {
        // Supprimer les blocs markdown
        String cleaned = response.replaceAll("(?s)```(?:dockerfile|docker)?(.*?)```", "$1");

        // Extraire depuis le premier FROM
        Matcher matcher = Pattern.compile("(?s)(FROM\\s+.+)").matcher(cleaned);
        if (!matcher.find()) return ERROR_DOCKERFILE;
        cleaned = matcher.group(1);

        // Couper la prose parasite après le Dockerfile (paragraphe qui suit une ligne vide)
        cleaned = cleaned.replaceAll("(?m)\n{2,}[A-Z][a-z].*(?s).*$", "");

        return cleaned.trim();
    }

    private record OllamaResponse(String response) {}
}
