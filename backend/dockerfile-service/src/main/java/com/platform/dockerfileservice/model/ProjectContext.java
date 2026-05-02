package com.platform.dockerfileservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the context of a project for Dockerfile generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectContext {

    private String language;
    private String version;
    private String buildTool;
    private String artifactName;
    private String framework;
    private String databaseType;
    private String healthEndpoint;
    private TargetOS targetOS;
    private int port;
    private List<String> filesToCopy;
    private List<String> dependencies;
    private boolean hasActuator;

    /**
     * Target Operating System for the Docker container.
     */
    public enum TargetOS {
        LINUX,
        WINDOWS
    }
}
