package com.user.dockerfileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    // Core
    private String language;
    private String javaVersion;
    private String springBootVersion;
    private String buildTool;
    private String framework;
    private String os;
    private String artifactId;
    private String version;
    private String artifactName; // Nom complet du JAR

    // Config & Port
    private int port;
    private String ddlAuto;
    private String healthEndpoint;
    private boolean hasCustomHealthEndpoint;
    private String databaseType;
    private String databaseName;
    private String databaseUsername;
    private String springProfile;
    private List<String> springProfiles;
    private List<String> extraEnvVars;
    private String datasourceUrl;
    private String artifactVersion;

    // Features
    private boolean hasActuator;
    private boolean hasLombok;
    private boolean hasDevtools;
    private boolean hasSecurity;
    private boolean hasValidation;
    private boolean hasThymeleaf;
    private boolean hasNativeImage;

    // Recommendations
    private String mavenImageRecommended;

    // Legacy compatibility
    private List<String> dependencies;
}
