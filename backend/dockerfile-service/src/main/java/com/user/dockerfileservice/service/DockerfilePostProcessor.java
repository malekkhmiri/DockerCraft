package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-processes raw LLM output to automatically fix known hallucination
 * patterns.
 *
 * Defence layers:
 * 1. clean() — strip markdown artefacts
 * 2. fix() — auto-correct the 6 known bad patterns
 * 3. isValid() — structural validation before returning
 * 4. buildFallback() — deterministic template if all else fails
 */
@Component
public class DockerfilePostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DockerfilePostProcessor.class);

    // ── Public API ─────────────────────────────────────────────────────

    public String process(String raw, AnalysisResult analysis) {
        String cleaned = clean(raw);
        String fixed = fix(cleaned, analysis);

        if (isValid(fixed, analysis)) {
            return fixed;
        }
        return buildFallback(analysis);
    }

    // ── Layer 1: Clean markdown artefacts ──────────────────────────────

    private String clean(String raw) {
        if (raw == null || raw.isBlank())
            return "";
        return raw
                .replaceAll("(?s)```[a-zA-Z]*\\n?", "") // strip opening fences
                .replaceAll("```", "") // strip closing fences
                .replaceAll("(?m)^#.*$", "") // strip inline comments
                .trim();
    }

    // ── Layer 2: Auto-fix known hallucination patterns ──────────────────

    private String fix(String d, AnalysisResult a) {
        String version = (a != null && a.getJavaVersion() != null) ? a.getJavaVersion() : "17";
        int port = (a != null && a.getPort() > 0) ? a.getPort() : 8080;

        // Fix 1: Wrong Maven image tag (jdk-XX instead of openjdk-XX-slim)
        d = d.replaceAll("maven:3\\.8\\.4-jdk-" + version, "maven:3.8.4-openjdk-" + version + "-slim");
        d = d.replaceAll("maven:(\\d+\\.\\d+\\.\\d+)-jdk-(\\d+)", "maven:$1-openjdk-$2-slim");

        // Fix 2: Remove ARG-based JAR references
        d = d.replaceAll("(?m)^ARG JAR_FILE.*$\\n?", "");
        d = d.replace("${JAR_FILE}", "target/*.jar");
        d = d.replace("$JAR_FILE", "target/*.jar");

        // Fix 4: curl replaced with wget
        d = d.replace("CMD curl -f", "CMD wget --quiet --tries=1 --spider");
        d = d.replace("curl -f http://", "wget --quiet --tries=1 --spider http://");

        // Fix 5: Ensure HEALTHCHECK uses reasonable timeouts
        d = d.replace("HEALTHCHECK --interval=10s --timeout=5s", "HEALTHCHECK --interval=30s --timeout=10s --retries=5");

        // Fix 6: Inject HEALTHCHECK if completely absent
        if (!d.contains("HEALTHCHECK")) {
            String healthPath = (a != null && a.isHasActuator()) ? "/actuator/health" : "/";
            String healthLine = "\nHEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n" +
                    "  CMD wget --quiet --tries=1 --spider http://localhost:" + port + healthPath + " || exit 1";
            if (d.contains("ENTRYPOINT")) {
                d = d.replaceFirst("(?m)^ENTRYPOINT", healthLine + "\nENTRYPOINT");
            } else if (d.contains("CMD")) {
                d = d.replaceFirst("(?m)^CMD", healthLine + "\nCMD");
            }
        }

        // Fix 7: Replace hallucinated parent-starter JAR
        d = d.replaceAll("COPY --from=builder (.*)/target/spring-boot-starter-parent.*\\.jar app\\.jar",
                Matcher.quoteReplacement("COPY --from=builder /build/target/*.jar app.jar"));

        // ─────────────────────────────────────────────────────────────────
        // BRUTAL LINE-BY-LINE ENFORCEMENT
        // ─────────────────────────────────────────────────────────────────
        String[] lines = d.split("\n");
        StringBuilder sb = new StringBuilder();
        int fromCount = 0;
        String jarName = "app.jar";

        for (String line : lines) {
            String trimmed = line.trim();
            
            // Fix 1: Force 'AS runtime' and Correct Java Version Mapping (1.8 -> 8)
            if (trimmed.startsWith("FROM ")) {
                String fixedFrom = trimmed.replace("1.8", "8"); // Catches -1.8 and :1.8
                fromCount++;
                if (fromCount == 2 && !fixedFrom.contains(" AS ")) {
                    sb.append(fixedFrom).append(" AS runtime\n");
                } else {
                    sb.append(fixedFrom).append("\n");
                }
                continue;
            }
            
            // Fix 2: Multi-module COPY logic (Optimized to avoid useless modules)
            if (trimmed.equals("COPY src ./src") && a != null && a.isMultiModule() && a.getModules() != null) {
                String targetModule = a.getModules().stream()
                        .filter(m -> m.contains("admin") || m.contains("web") || m.contains("api") || m.contains("portal"))
                        .findFirst().orElse(null);
                
                for (String module : a.getModules()) {
                    // Only copy target module and "core" modules (common, security, mbg, etc.)
                    boolean isCore = module.contains("common") || module.contains("security") || module.contains("mbg") || module.contains("core");
                    if (module.equals(targetModule) || isCore) {
                        sb.append("COPY ").append(module).append(" ./").append(module).append("\n");
                    }
                }
                continue;
            }

            // Fix 3: Multi-module Build Command & JAR Path
            if (a != null && a.isMultiModule()) {
                String targetModule = a.getModules().stream()
                        .filter(m -> m.contains("admin") || m.contains("web") || m.contains("api") || m.contains("portal"))
                        .findFirst().orElse(null);

                if (targetModule != null) {
                    // Update build command
                    if (trimmed.startsWith("RUN mvn clean package") && !trimmed.contains("-pl")) {
                        sb.append("RUN mvn clean package -DskipTests -B -pl ").append(targetModule).append(" -am\n");
                        continue;
                    }
                    // Update JAR copy path
                    if (trimmed.contains("COPY --from=builder") && trimmed.contains("/target/")) {
                        sb.append("COPY --from=builder --chown=spring:spring /build/").append(targetModule).append("/target/*.jar app.jar\n");
                        continue;
                    }
                }
            }

            // Fix 4: Force shell form for ENTRYPOINT
            if (trimmed.startsWith("ENTRYPOINT ")) {
                sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar ").append(jarName).append("\"]\n");
                continue;
            }

            // Fix 5: Standardize common ENV lines
            if (trimmed.contains("SPRING_DATASOURCE_URL=") && a != null && a.getDatabaseName() != null) {
                String dbName = a.getDatabaseName();
                String dbType = (a.getDatabaseType() != null) ? a.getDatabaseType() : "mysql";
                int dbPort = switch (dbType.toLowerCase()) {
                    case "mysql", "mariadb" -> 3306;
                    case "mongodb" -> 27017;
                    default -> 5432;
                };
                String correctUrl = "jdbc:" + dbType + "://db:" + dbPort + "/" + dbName;
                sb.append("    SPRING_DATASOURCE_URL=").append(correctUrl).append(" \\\n");
                continue;
            }

            sb.append(line).append("\n");
        }
        d = sb.toString();

        // 4. Healthcheck path enforcement (Default to actuator for Spring)
        if (a != null && d.contains("HEALTHCHECK")) {
            String targetPath = a.isHasActuator() ? "/actuator/health" : (a.getHealthEndpoint() != null ? a.getHealthEndpoint() : "/actuator/health");
            d = d.replaceAll("(?m)(CMD wget .* http://localhost:\\d+)(/\\S*)", "$1" + targetPath);
            if (d.contains("http://localhost:" + port + "/ ")) {
                 d = d.replace("http://localhost:" + port + "/ ", "http://localhost:" + port + targetPath + " ");
            }
        }

        return d;
    }

    // ── Layer 3: Structural validation ─────────────────────────────────

    private boolean isValid(String d, AnalysisResult a) {
        if (d == null || d.isBlank())
            return false;
        int port = (a != null && a.getPort() > 0) ? a.getPort() : 8080;

        return countOccurrences(d, "FROM ") >= 2
                && d.contains("COPY --from=")
                && d.contains("EXPOSE " + port)
                && d.contains("ENTRYPOINT")
                && d.contains("USER ")
                && !d.contains("```")
                && !d.contains("<version>")
                && !d.contains("[REPLACE")
                && !d.matches("(?s).*maven:\\d+\\.\\d+\\.\\d+-jdk-\\d+.*"); // no bad image tag
    }

    // ── Layer 4: Deterministic fallback ────────────────────────────────

    private String buildFallback(AnalysisResult a) {
        boolean isMaven = a == null || "maven".equals(a.getBuildTool());
        String version = (a != null && a.getJavaVersion() != null) ? a.getJavaVersion() : "17";
        int port = (a != null && a.getPort() > 0) ? a.getPort() : 8080;
        String buildCmd = isMaven ? "mvn clean package -DskipTests -B" : "./gradlew build -x test";
        String copyPath = isMaven ? "target/*.jar" : "build/libs/*.jar";
        String builder = getMavenImage(a);
        String runtime = getRuntimeImage(a);
        String healthPath = (a != null && a.isHasActuator()) ? "/actuator/health" : "/";
        
        // Use reconstructed values for fallback
        String dbUrl = null;
        String dbUser = "root";
        if (a != null && a.getDatabaseName() != null) {
            String type = (a.getDatabaseType() != null) ? a.getDatabaseType() : "mysql";
            int dbPort = "mysql".equalsIgnoreCase(type) ? 3306 : 5432;
            dbUrl = "jdbc:" + type + "://db:" + dbPort + "/" + a.getDatabaseName();
            dbUser = (a.getDatabaseUsername() != null) ? a.getDatabaseUsername() : "root";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FROM ").append(builder).append(" AS builder\n");
        sb.append("WORKDIR /build\n");
        sb.append(isMaven ? "COPY pom.xml .\nRUN mvn dependency:go-offline -B\n" : "COPY build.gradle settings.gradle* .\nRUN ./gradlew dependencies --no-daemon\n");
        sb.append("COPY src ./src\n");
        sb.append("RUN ").append(buildCmd).append("\n\n");
        
        // Stage 2
        sb.append("FROM ").append(runtime).append(" AS runtime\n");
        sb.append("RUN addgroup -S spring \\\n");
        sb.append("    && adduser -S spring -G spring \\\n");
        sb.append("    && apk add --no-cache wget\n");
        sb.append("USER spring:spring\n");
        sb.append("WORKDIR /app\n");

        // Build ENV block using analyzed values
        sb.append("ENV SPRING_PROFILES_ACTIVE=prod \\\n");
        sb.append("    JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"");
        if (dbUrl != null) {
            sb.append(" \\\n    SPRING_DATASOURCE_URL=").append(dbUrl);
            sb.append(" \\\n    SPRING_DATASOURCE_USERNAME=").append(dbUser);
            sb.append(" \\\n    SPRING_DATASOURCE_PASSWORD=CHANGE_ME_INJECT_AT_RUNTIME");
        }
        sb.append("\n");

        sb.append("COPY --from=builder --chown=spring:spring ").append(copyPath).append(" app.jar\n");
        sb.append("EXPOSE ").append(port).append("\n");

        sb.append("HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n");
        sb.append("  CMD wget --quiet --tries=1 --spider http://localhost:").append(port).append(healthPath).append(" || exit 1\n");

        // ✅ ALWAYS use shell form so $JAVA_OPTS is interpreted
        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar app.jar\"]\n");
        return sb.toString();
    }

    // ── Image helpers ──────────────────────────────────────────────────

    private String getMavenImage(AnalysisResult a) {
        String v = (a != null && a.getJavaVersion() != null) ? a.getJavaVersion() : "17";
        return switch (v) {
            case "21" -> "maven:3.9.6-eclipse-temurin-21-alpine";
            case "11" -> "maven:3.8.4-openjdk-11-slim";
            case "1.8", "8" -> "maven:3.9.6-eclipse-temurin-8-alpine";
            default -> "maven:3.8.4-openjdk-17-slim";
        };
    }

    private String getRuntimeImage(AnalysisResult a) {
        if (a != null && a.isHasNativeImage())
            return "ubuntu:22.04";
        String v = (a != null && a.getJavaVersion() != null) ? a.getJavaVersion() : "17";
        return switch (v) {
            case "21" -> "eclipse-temurin:21-jre-alpine";
            case "11" -> "eclipse-temurin:11-jre-alpine";
            case "1.8", "8" -> "eclipse-temurin:8-jre-alpine";
            default -> "eclipse-temurin:17-jre-alpine";
        };
    }

    // ── Utility ────────────────────────────────────────────────────────

    private int countOccurrences(String text, String pattern) {
        return text.split(Pattern.quote(pattern), -1).length - 1;
    }
}
