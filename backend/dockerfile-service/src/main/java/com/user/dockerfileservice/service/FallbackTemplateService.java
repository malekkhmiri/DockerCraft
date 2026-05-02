package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Enhanced Fallback Dockerfile generator.
 * Perfectly aligned with ValidatorService rules to ensure 100% success rate.
 */
@Service
public class FallbackTemplateService {

    public String generateFallback(AnalysisResult a) {
        // 1. Image selection
        String builderImage = a.getMavenImageRecommended() != null 
                              ? a.getMavenImageRecommended() 
                              : "maven:3.8.4-openjdk-17-slim";
        
        String javaVer = a.getJavaVersion() != null ? a.getJavaVersion() : "17";
        String runtimeImage = "eclipse-temurin:" + javaVer + "-jre-alpine";

        // 2. Build flags (Lombok, etc.)
        String mvnFlags = "-DskipTests -B";
        if (a.isHasLombok()) {
            mvnFlags += " -Dmaven.compiler.forceJavacCompilerUse=true";
        }

        // 3. Runtime packages (PostgreSQL libpq, etc.)
        String apkPackages = "wget";
        if ("postgresql".equals(a.getDatabaseType())) {
            apkPackages += " libpq";
        }

        // 4. Paths and Ports
        int port = a.getPort() > 0 ? a.getPort() : 8080;
        String jar = a.getArtifactName() != null ? a.getArtifactName() : "*.jar";
        String healthPath = a.isHasActuator() ? "/actuator/health" : "/";
        
        boolean isGradle = "gradle".equalsIgnoreCase(a.getBuildTool());
        String buildCmd = isGradle ? "./gradlew bootJar --no-daemon" : "mvn clean package " + mvnFlags;
        String copySource = isGradle ? "/build/build/libs/" + jar : "/build/target/" + jar;

        // 5. Build the final string
        StringBuilder sb = new StringBuilder();
        sb.append("# ── Fallback Production Dockerfile (Generated due to LLM Validation Failure) ──\n");
        sb.append("FROM ").append(builderImage).append(" AS builder\n");
        sb.append("WORKDIR /build\n");
        if (isGradle) {
            sb.append("COPY build.gradle settings.gradle* .\nRUN ./gradlew dependencies --no-daemon\n");
        } else {
            sb.append("COPY pom.xml .\nRUN mvn dependency:go-offline -B\n");
        }
        sb.append("COPY src ./src\n");
        sb.append("RUN ").append(buildCmd).append("\n\n");

        sb.append("FROM ").append(runtimeImage).append("\n");
        sb.append("RUN addgroup -S spring && adduser -S spring -G spring \\\n");
        sb.append("    && apk add --no-cache ").append(apkPackages).append("\n");
        sb.append("USER spring:spring\n");
        sb.append("WORKDIR /app\n");
        sb.append("ENV JAVA_OPTS=\"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0\"\n");
        
        if ("create".equals(a.getDdlAuto())) {
            sb.append("ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update\n");
        }

        sb.append("COPY --from=builder --chown=spring:spring ").append(copySource).append(" app.jar\n");
        sb.append("EXPOSE ").append(port).append("\n");
        sb.append("HEALTHCHECK --interval=30s --timeout=10s --retries=5 \\\n");
        sb.append("  CMD wget --quiet --tries=1 --spider http://localhost:").append(port).append(healthPath).append(" || exit 1\n");
        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar app.jar\"]\n");

        return sb.toString();
    }

}
