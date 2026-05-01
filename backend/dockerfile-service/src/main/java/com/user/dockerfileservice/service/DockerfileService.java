package com.user.dockerfileservice.service;

import com.user.dockerfileservice.entity.Dockerfile;
import java.util.List;

public interface DockerfileService {
    void generateDockerfile(Long projectId);
    String generate(com.user.dockerfileservice.dto.AnalysisResult analysis);
    Dockerfile getByProjectId(Long projectId);

    Dockerfile getById(Long id);
    List<Dockerfile> getAll();
    Dockerfile updateDockerfile(Long id, String content);
    List<Dockerfile> getHistoryByProjectId(Long projectId);
    long count();
}
