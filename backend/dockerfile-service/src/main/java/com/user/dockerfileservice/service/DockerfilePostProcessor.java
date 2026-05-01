package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

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
        d = d.replaceAll(
                "maven:3\\.8\\.4-jdk-" + version,
                "maven:3.8.4-openjdk-" + version + "-slim");
        // Generic catch for any maven:X.X.X-jdk-XX pattern
        d = d.replaceAll(
                "maven:(\\d+\\.\\d+\\.\\d+)-jdk-(\\d+)",
                "maven:$1-openjdk-$2-slim");

        // Fix 2: Remove ARG-based JAR references — replace with direct path
        d = d.replaceAll("(?m)^ARG JAR_FILE.*$\\n?", "");
        d = d.replaceAll("\\$\\{JAR_FILE\\}", "target/*.jar");
        d = d.replaceAll("\\$JAR_FILE", "target/*.jar");

        // Fix 3: ENV JAVA_OPTS without quotes
        d = d.replaceAll(
                "ENV JAVA_OPTS=(-XX[^\"\\n]+)",
                "ENV JAVA_OPTS=\"$1\"");

        // Fix 4: curl replaced with wget (curl not on Alpine JRE images)
        d = d.replaceAll("CMD curl -f", "CMD wget --quiet --tries=1 --spider");
        d = d.replaceAll("curl -f http://", "wget --quiet --tries=1 --spider http://");

        // Fix 5: Ensure HEALTHCHECK uses reasonable timeouts
        d = d.replaceAll(
                "HEALTHCHECK --interval=10s --timeout=5s",
                "HEALTHCHECK --interval=30s --timeout=10s --retries=5");

        // Fix 6: Inject HEALTHCHECK if completely absent (before ENTRYPOINT or CMD)
        if (!d.contains("HEALTHCHECK")) {
            String healthPath = (a != null && a.isHasActuator()) ? "/actuator/health" : "/";
            String healthLine = "\nHEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n" +
                    "  CMD wget --quiet --tries=1 --spider http://localhost:" + port + healthPath + " || exit 1";
            if (d.contains("ENTRYPOINT")) {
                d = d.replaceAll("(?m)^(ENTRYPOINT)", healthLine + "\n$1");
            } else if (d.contains("CMD")) {
                d = d.replaceAll("(?m)^(CMD)", healthLine + "\n$1");
            }
        }

        // Fix 7: MANDATORY - Replace any parent-starter JAR reference in COPY with a
        // wildcard
        // If the LLM tries to copy spring-boot-starter-parent-x.x.x.jar, it's a
        // hallucination.
        d = d.replaceAll(
                "COPY --from=builder (.*)/target/spring-boot-starter-parent.*\\.jar app\\.jar",
                "COPY --from=builder $1/target/*.jar app.jar");

        // Fix 8: Correct Healthcheck path to match analysis result exactly
        String targetHealth = (a != null) ? a.getHealthEndpoint() : "/";
        if (targetHealth == null) targetHealth = "/"; // fallback de sécurité
        
        if (d.contains("/actuator/health") && !"/actuator/health".equals(targetHealth)) {
             d = d.replace("/actuator/health", targetHealth);
        }
        if (d.contains("/api/health") && !"/api/health".equals(targetHealth)) {
             d = d.replace("/api/health", targetHealth);
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
        boolean isMaven = !"gradle".equalsIgnoreCase(a != null ? a.getBuildTool() : "maven");
        String builder = isMaven ? getMavenImage(a) : "gradle:8.5-jdk17-alpine";
        String runtime = getRuntimeImage(a);
        String buildCmd = isMaven ? "mvn clean package -DskipTests -B"
                : "./gradlew bootJar --no-daemon";
        String copyPath = isMaven ? "/build/target/*.jar" : "/build/build/libs/*.jar";
        int port = (a != null && a.getPort() > 0) ? a.getPort() : 8080;
        boolean actuator = a != null && a.isHasActuator();

        StringBuilder sb = new StringBuilder();
        sb.append("FROM ").append(builder).append(" AS builder\n");
        sb.append("WORKDIR /build\n");
        sb.append(isMaven ? "COPY pom.xml .\nRUN mvn dependency:go-offline -B\n"
                : "COPY build.gradle settings.gradle* .\nRUN ./gradlew dependencies --no-daemon\n");
        sb.append("COPY src ./src\n");
        sb.append("RUN ").append(buildCmd).append("\n\n");

        sb.append("FROM ").append(runtime).append("\n");
        sb.append("RUN addgroup -S spring \\\n");
        sb.append("    && adduser -S spring -G spring \\\n");
        sb.append("    && apk add --no-cache wget\n");
        sb.append("USER spring:spring\n");
        sb.append("WORKDIR /app\n");
        sb.append("ENV JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"\n");
        sb.append("COPY --from=builder --chown=spring:spring ").append(copyPath).append(" app.jar\n");
        sb.append("EXPOSE ").append(port).append("\n");

        String healthPath = (a != null && a.isHasActuator()) ? "/actuator/health" : "/";
        sb.append("HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n");
        sb.append("  CMD wget --quiet --tries=1 --spider http://localhost:")
                .append(port).append(healthPath).append(" || exit 1\n");

        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar app.jar\"]\n");
        return sb.toString();
    }

    // ── Image helpers ──────────────────────────────────────────────────

    private String getMavenImage(AnalysisResult a) {
        String v = (a != null && a.getJavaVersion() != null) ? a.getJavaVersion() : "17";
        return switch (v) {
            case "21" -> "maven:3.9.6-eclipse-temurin-21-alpine";
            case "11" -> "maven:3.8.4-openjdk-11-slim";
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
            default -> "eclipse-temurin:17-jre-alpine";
        };
    }

    // ── Utility ────────────────────────────────────────────────────────

    private int countOccurrences(String text, String pattern) {
        return text.split(Pattern.quote(pattern), -1).length - 1;
    }
}
