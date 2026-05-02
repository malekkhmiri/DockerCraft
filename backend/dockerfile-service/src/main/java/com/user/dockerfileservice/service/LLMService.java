package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import com.user.dockerfileservice.util.DebugLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private static final String USER_TURN =
            "Generate the Dockerfile for this project now. " +
            "Output only the Dockerfile, nothing else. " +
            "Always use explicit backslash continuation (\\) for multi-line RUN commands.";

    private static final String ERROR_DOCKERFILE = """
            # Erreur lors de la génération
            FROM alpine
            CMD ["echo", "error"]
            """;

    private static final Set<String> DOCKERFILE_KEYWORDS = Set.of(
            "FROM", "RUN", "COPY", "ADD", "ENV", "EXPOSE", "HEALTHCHECK",
            "ENTRYPOINT", "CMD", "WORKDIR", "ARG", "LABEL", "USER",
            "VOLUME", "STOPSIGNAL", "ONBUILD", "SHELL", "#");

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

    // ── API publique ───────────────────────────────────────────────────────────

    public String generate(AnalysisResult raw) {
        AnalysisResult analysis = normalize(raw);
        String systemPrompt = buildSystemPrompt(analysis);
        logger.info("🚀 Génération pour {} — JAR: {} (Java {}, DB {}, Framework {})",
                analysis.getArtifactId(),
                analysis.getArtifactName(),
                analysis.getJavaVersion(),
                analysis.getDatabaseType() != null ? analysis.getDatabaseType() : "none",
                analysis.getFramework());
        return callOllama(systemPrompt);
    }

    // ── Normalisation ──────────────────────────────────────────────────────────

    private AnalysisResult normalize(AnalysisResult raw) {
        return AnalysisResult.builder()
                .artifactId(raw.getArtifactId())
                .artifactName(raw.getArtifactName()  != null ? raw.getArtifactName()  : "app.jar")
                .javaVersion(raw.getJavaVersion()    != null ? raw.getJavaVersion()    : "17")
                .framework(raw.getFramework()        != null ? raw.getFramework()      : "spring-boot")
                .databaseType(raw.getDatabaseType())
                .healthEndpoint(raw.getHealthEndpoint()) // null = pas de healthcheck, intentionnel
                .build();
    }

    // ── Construction du prompt ─────────────────────────────────────────────────

    private String buildSystemPrompt(AnalysisResult a) {
        String javaVer   = a.getJavaVersion();
        String framework = a.getFramework();
        String dbType    = a.getDatabaseType();
        String artifact  = a.getArtifactName();
        String health    = a.getHealthEndpoint();

        boolean isPlain   = "java-plain".equalsIgnoreCase(framework);
        boolean hasDb     = dbType != null
                         && !dbType.equalsIgnoreCase("h2")
                         && !dbType.equalsIgnoreCase("none");
        boolean hasHealth = health != null && !health.isBlank();

        StringBuilder sb = new StringBuilder();

        sb.append("""
            You are a professional Dockerfile generator. Your ONLY output is a valid, production-ready Dockerfile.
            No explanations. No markdown. Start directly with FROM.

            """);

        sb.append("## Project Facts\n")
          .append("- Framework : ").append(framework).append("\n")
          .append("- Java      : ").append(javaVer).append("\n")
          .append("- Database  : ").append(dbType != null ? dbType : "none").append("\n")
          .append("- JAR name  : ").append(artifact).append("\n")
          .append("- Health URL: ").append(hasHealth ? health : "none").append("\n\n");

        if ("multi-module".equalsIgnoreCase(framework)) {
            return """
                # ERROR: Multi-module Maven project detected.
                # This project contains multiple deployable sub-modules.
                # Please select the specific module to dockerize (e.g., mall-admin, mall-portal).
                # Current system generates Dockerfiles for single-artifact projects only.
                """;
        }

        if (isPlain) {
            String jarBaseName = artifact.replace(".jar", "");
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
            Build stage rules (STRICT):
            - FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
            - WORKDIR /build
            - COPY pom.xml .
            - RUN mvn dependency:go-offline -B
            - COPY src ./src
            - RUN mvn clean package -DskipTests -B
            - DO NOT CHAIN 'COPY src ./src' with any RUN command using '&&'.

            Runtime stage rules (STRICT):
            - FROM eclipse-temurin:%s-jre-alpine AS runtime
            - RUN addgroup -S spring && adduser -S spring -G spring
            - USER spring:spring
            - WORKDIR /app
            - Use the 'ENV' keyword for every variable declaration.
            - ENV SPRING_PROFILES_ACTIVE=prod \\
                  JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            - COPY --from=builder --chown=spring:spring /build/target/%s app.jar
            - EXPOSE 8080
            - ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
            """.formatted(javaVer, javaVer, artifact));

        if (hasDb) {
            String jdbcUrl = dbType == null ? "jdbc:h2:mem:testdb" : switch (dbType.toLowerCase()) {
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

                ## Healthcheck
                Update the existing RUN addgroup line to include wget:
                RUN addgroup -S spring && adduser -S spring -G spring && apk add --no-cache wget

                Then add:
                HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\
                  CMD wget --quiet --tries=1 --spider http://localhost:8080%s || exit 1
                """.formatted(health));
        } else {
            sb.append("\n## No healthcheck — DO NOT add HEALTHCHECK or wget.\n");
        }

        return sb.toString();
    }

    // ── Appel Ollama ───────────────────────────────────────────────────────────

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

    // ── Nettoyage de la réponse ────────────────────────────────────────────────

    private String cleanResponse(String response) {
        String cleaned = response.replaceAll("(?s)```(?:dockerfile|docker)?(.*?)```", "$1");

        Matcher matcher = Pattern.compile("(?s)(FROM\\s+.+)").matcher(cleaned);
        if (!matcher.find()) return ERROR_DOCKERFILE;

        return Arrays.stream(matcher.group(1).split("\n", -1))
                .filter(line -> {
                    String t = line.stripLeading();
                    if (t.isBlank() || t.startsWith("&&") || t.startsWith("\\")) return true;
                    String firstWord = t.split("\\s+", 2)[0];
                    return DOCKERFILE_KEYWORDS.contains(firstWord);
                })
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private record OllamaResponse(String response) {}
}
