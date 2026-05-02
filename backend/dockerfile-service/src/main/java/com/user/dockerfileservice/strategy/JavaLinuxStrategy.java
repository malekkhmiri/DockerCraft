package com.user.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JavaLinuxStrategy implements LanguageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JavaLinuxStrategy.class);

    @Override
    public String getSupportedLanguage() { return "java"; }

    private int getDefaultDbPort(String dbType) {
        if (dbType == null) return 5432;
        return switch (dbType.toLowerCase()) {
            case "mysql", "mariadb" -> 3306;
            case "mongodb"          -> 27017;
            default                 -> 5432;
        };
    }

    private String getDefaultDbUsername(String dbType) {
        if (dbType == null) return "postgres";
        return switch (dbType.toLowerCase()) {
            case "mysql", "mariadb" -> "root";
            case "mongodb"          -> "admin";
            default                 -> "postgres";
        };
    }

    private String getJdbcPrefix(String dbType) {
        if (dbType == null) return "jdbc:postgresql";
        return switch (dbType.toLowerCase()) {
            case "mongodb" -> "mongodb";
            default        -> "jdbc:" + dbType.toLowerCase();
        };
    }

    /**
     * Returns true if the value is a placeholder (e.g. [SECURE_URL], ${...}, <...>)
     * and should not be used as a real configuration value.
     */
    private boolean isPlaceholder(String value) {
        return value == null
            || value.isBlank()
            || value.startsWith("[")
            || value.startsWith("${")
            || value.startsWith("<");
    }

    /**
     * Builds the ENV block that will be embedded verbatim in the LLM prompt.
     * This is the source of truth — the LLM is instructed to copy it exactly.
     * Detects placeholder values and falls back to reconstructed values.
     */
    private String buildEnvBlock(AnalysisResult a) {
        String dbType = a.getDatabaseType();

        // ── Resolve JDBC URL ──────────────────────────────────────────────
        String rawUrl = a.getDatasourceUrl();
        String jdbcUrl;
        if (!isPlaceholder(rawUrl)) {
            // Real URL found — normalise localhost → db for container networking
            jdbcUrl = rawUrl.replace("localhost", "db");
            logger.info("[PROMPT] Using analysed URL: {}", jdbcUrl);
        } else if (dbType != null && !isPlaceholder(a.getDatabaseName())) {
            // URL was masked/placeholder — reconstruct from type + DB name
            int dbPort = getDefaultDbPort(dbType);
            String prefix = getJdbcPrefix(dbType);
            jdbcUrl = prefix + "://db:" + dbPort + "/" + a.getDatabaseName();
            logger.info("[PROMPT] URL was placeholder. Reconstructed: {}", jdbcUrl);
        } else {
            jdbcUrl = null; // genuinely unknown
        }

        // ── Resolve Username ──────────────────────────────────────────────
        String rawUser = a.getDatabaseUsername();
        String username;
        if (!isPlaceholder(rawUser)) {
            username = rawUser;
            logger.info("[PROMPT] Using analysed username: {}", username);
        } else {
            username = getDefaultDbUsername(dbType);
            logger.info("[PROMPT] Username was placeholder. Defaulting to: {}", username);
        }

        // ── Build ENV block ───────────────────────────────────────────────
        StringBuilder env = new StringBuilder("ENV SPRING_PROFILES_ACTIVE=prod \\\n");
        env.append("    JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"");
        if (jdbcUrl != null) {
            env.append(" \\\n    SPRING_DATASOURCE_URL=").append(jdbcUrl);
            env.append(" \\\n    SPRING_DATASOURCE_USERNAME=").append(username);
            env.append(" \\\n    SPRING_DATASOURCE_PASSWORD=CHANGE_ME_INJECT_AT_RUNTIME");
        }
        return env.toString();
    }

    @Override
    public String generatePrompt(AnalysisResult a) {
        String java    = a.getJavaVersion() != null ? a.getJavaVersion() : "17";
        int    port    = a.getPort() > 0 ? a.getPort() : 8080;
        String health  = a.getHealthEndpoint() != null ? a.getHealthEndpoint() : "/";
        String envBlock = buildEnvBlock(a);

        // Healthcheck line — deterministic, based on actuator detection
        String healthcheckLine = "HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n" +
                "  CMD wget --quiet --tries=1 --spider http://localhost:%d%s || exit 1".formatted(port, health);

        String multiModuleNote = "";
        if (a.isMultiModule() && a.getModules() != null) {
            multiModuleNote = "\nIMPORTANT: This is a multi-module project containing: " + a.getModules() +
                    ". Build from the project root. The JAR will be in a sub-module target/ folder.";
        }

        return """
                Generate a production-ready 2-stage Dockerfile for a Java Spring Boot microservice.

                == CONTEXT ==
                Java version : %s
                Exposed port : %d
                Build tool   : Maven%s

                == OUTPUT STRUCTURE ==
                You MUST produce exactly this structure, copying the blocks verbatim:

                ### Stage 1 — Build
                FROM maven:3.9.6-eclipse-temurin-%s-alpine AS builder
                WORKDIR /build
                COPY pom.xml .
                RUN mvn dependency:go-offline -B
                COPY src ./src
                RUN mvn clean package -DskipTests -B

                ### Stage 2 — Runtime
                FROM eclipse-temurin:%s-jre-alpine
                RUN addgroup -S spring && adduser -S spring -G spring \\
                    && apk add --no-cache wget
                USER spring:spring
                WORKDIR /app
                %s
                COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar
                EXPOSE %d
                %s
                ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

                == STRICT RULES ==
                1. Copy the ENV block EXACTLY as shown above. Do NOT change any value.
                2. The ENTRYPOINT MUST be ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] — never exec form.
                3. Use "USER spring:spring" — never run as root.
                4. Copy the HEALTHCHECK line EXACTLY as shown.
                5. No markdown, no explanations, no backticks. Output ONLY the Dockerfile.
                """.formatted(
                    java, port, multiModuleNote,
                    java, java,
                    envBlock,
                    port,
                    healthcheckLine
                );
    }
}