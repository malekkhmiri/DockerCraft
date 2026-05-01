package com.platform.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class JavaLinuxStrategy implements LanguageStrategy {

    @Override
    public String getSupportedLanguage() {
        return "java";
    }

    private int getDefaultDbPort(String dbType) {
        if (dbType == null) return 5432;
        return switch (dbType.toLowerCase()) {
            case "mysql", "mariadb" -> 3306;
            case "mongodb"          -> 27017;
            default                 -> 5432;
        };
    }

    // ✅ Username par défaut selon le type de DB
    private String getDefaultDbUsername(String dbType) {
        if (dbType == null) return "postgres";
        return switch (dbType.toLowerCase()) {
            case "mysql", "mariadb" -> "root";
            case "mongodb"          -> "admin";
            default                 -> "postgres";
        };
    }

    // ✅ Préfixe JDBC correct selon le type de DB
    private String getJdbcPrefix(String dbType) {
        if (dbType == null) return "jdbc:postgresql";
        return switch (dbType.toLowerCase()) {
            case "mongodb" -> "mongodb";          // pas de jdbc:
            default        -> "jdbc:" + dbType.toLowerCase();
        };
    }

    private String buildEnvBlock(AnalysisResult a) {
        String dbType = a.getDatabaseType();
        String dbName = a.getDatabaseName() != null ? a.getDatabaseName() : "app_db";
        StringBuilder env = new StringBuilder("ENV \\\n");
        env.append("    SPRING_PROFILES_ACTIVE=prod \\\n");
        env.append("    JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"");

        if (dbType != null) {
            int port = getDefaultDbPort(dbType);
            String prefix = getJdbcPrefix(dbType);
            String user = getDefaultDbUsername(dbType);
            env.append(" \\\n    SPRING_DATASOURCE_URL=")
               .append(prefix).append("://db:").append(port).append("/").append(dbName);
            env.append(" \\\n    SPRING_DATASOURCE_USERNAME=").append(user);
            env.append(" \\\n    SPRING_DATASOURCE_PASSWORD=CHANGE_ME_INJECT_AT_RUNTIME");
        }
        return env.toString();
    }

    @Override
    public String generatePrompt(AnalysisResult a) {
        String java = a.getJavaVersion() != null ? a.getJavaVersion() : "17";
        int port = a.getPort() > 0 ? a.getPort() : 8080;
        String healthPath = a.getHealthEndpoint();

        // ✅ Génération déterministe du bloc Healthcheck
        String healthcheckBlock;
        if (healthPath != null) {
            healthcheckBlock = """
                HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\
                  CMD wget --quiet --tries=1 --spider http://localhost:%d%s || exit 1
                """.formatted(port, healthPath);
        } else {
            healthcheckBlock = "# No suitable health endpoint detected - Skipping HEALTHCHECK";
        }

        return """
                Task: Generate a 2-stage Dockerfile for a Spring Boot app.
                
                Mandatory Content (COPY VERBATIM):
                1. BUILDER: maven:3.9.6-eclipse-temurin-%s-alpine
                2. RUNTIME: eclipse-temurin:%s-jre-alpine
                3. ENV BLOCK (copy exactly, no reordering):
                %s

                Strict Rules (Zero deviation allowed):
                - RULE 1: Use stage names 'builder' and 'runtime'.
                - RULE 2: In builder, 'RUN mvn dependency:go-offline -B' before 'COPY src'.
                - RULE 3: Wildcard JAR copy:
                  COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar
                - RULE 4: Use this HEALTHCHECK block exactly as provided:
                %s
                - RULE 5: No hardcoded source/target compiler flags.
                - RULE 6: Create user 'spring' and use 'USER spring:spring'.
                - RULE 7: ENV block exactly as provided, no reordering.

                Output ONLY the Dockerfile. No markdown, no explanation, no backticks.
                """.formatted(java, java, buildEnvBlock(a), healthcheckBlock);
    }
}