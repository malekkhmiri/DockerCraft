package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

/**
 * Legacy builder — kept for potential direct use but superseded by the
 * adaptive strategy + LLM pipeline. All field references updated to match
 * the current AnalysisResult model.
 */
@Component
public class JavaDockerfileBuilder {

    public String build(AnalysisResult analysis) {
        StringBuilder sb = new StringBuilder();
        appendBuilderStage(sb, analysis);
        sb.append("\n");
        appendRuntimeStage(sb, analysis);
        return sb.toString();
    }

    // ── Stage 1: Build ─────────────────────────────────────────────────

    private void appendBuilderStage(StringBuilder sb, AnalysisResult a) {
        boolean isMaven = !"gradle".equalsIgnoreCase(a.getBuildTool());
        sb.append("FROM ").append(getMavenImage(a.getJavaVersion())).append(" AS builder\n");
        sb.append("WORKDIR /build\n");

        if (isMaven) {
            sb.append("COPY pom.xml .\n");
            sb.append("RUN mvn dependency:go-offline -B\n");
            sb.append("COPY src ./src\n");
            sb.append("RUN ").append(getMavenBuildCmd(a)).append("\n");
        } else {
            sb.append("COPY build.gradle settings.gradle* .\n");
            sb.append("RUN ./gradlew dependencies --no-daemon\n");
            sb.append("COPY src ./src\n");
            sb.append("RUN ./gradlew bootJar --no-daemon\n");
        }
    }

    // ── Stage 2: Runtime ───────────────────────────────────────────────

    private void appendRuntimeStage(StringBuilder sb, AnalysisResult a) {
        int    port     = a.getPort() > 0 ? a.getPort() : 8080;
        boolean isMaven = !"gradle".equalsIgnoreCase(a.getBuildTool());
        String  jarSrc  = isMaven ? "/build/target/*.jar" : "/build/build/libs/*.jar";
        String  health  = a.isHasActuator() ? "/actuator/health" : "/";

        sb.append("FROM ").append(getRuntimeImage(a)).append("\n");
        sb.append("RUN addgroup -S spring \\\n");
        sb.append("    && adduser -S spring -G spring \\\n");
        sb.append("    && apk add --no-cache wget\n");
        sb.append("USER spring:spring\n");
        sb.append("WORKDIR /app\n");
        sb.append("ENV JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"\n");
        sb.append("COPY --from=builder --chown=spring:spring ").append(jarSrc).append(" app.jar\n");
        sb.append("EXPOSE ").append(port).append("\n");
        sb.append("HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n");
        sb.append("  CMD wget --quiet --tries=1 --spider http://localhost:")
          .append(port).append(health).append(" || exit 1\n");
        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar app.jar\"]\n");
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String getMavenBuildCmd(AnalysisResult a) {
        // Quarkus native → special flag; everything else → standard Spring Boot
        if ("quarkus".equalsIgnoreCase(a.getFramework()) && a.isHasNativeImage()) {
            return "mvn clean package -Pnative -DskipTests -B";
        }
        return "mvn clean package -DskipTests -B";
    }

    private String getMavenImage(String v) {
        return switch (v != null ? v : "17") {
            case "21" -> "maven:3.9.6-eclipse-temurin-21-alpine";
            case "11" -> "maven:3.8.4-openjdk-11-slim";
            default   -> "maven:3.8.4-openjdk-17-slim";
        };
    }

    private String getRuntimeImage(AnalysisResult a) {
        if (a.isHasNativeImage()) return "ubuntu:22.04";
        return switch (a.getJavaVersion() != null ? a.getJavaVersion() : "17") {
            case "21" -> "eclipse-temurin:21-jre-alpine";
            case "11" -> "eclipse-temurin:11-jre-alpine";
            default   -> "eclipse-temurin:17-jre-alpine";
        };
    }
}
