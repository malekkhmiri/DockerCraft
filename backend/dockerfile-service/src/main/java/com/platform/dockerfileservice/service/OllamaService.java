package com.platform.dockerfileservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.dockerfileservice.exception.DockerfileGenerationException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service to interact with the local Ollama LLM.
 */
@Service
public class OllamaService {

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5-coder:7b-instruct}")
    private String model;

    private final RestTemplate restTemplate;

    public OllamaService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generates a Dockerfile using the Ollama LLM with RAG context.
     *
     * @param basePrompt the base strategy prompt
     * @param successfulExamples successful past Dockerfiles
     * @param failedExamples failed past Dockerfiles with errors
     * @return the generated Dockerfile content
     */
    public String generateDockerfile(String basePrompt, String successfulExamples, String failedExamples) {
        String url = ollamaUrl + "/api/generate";

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append("You are a Senior DevOps Architect specializing in optimized, production-ready Dockerfiles.\n\n");
        
        if (successfulExamples != null && !successfulExamples.isEmpty()) {
            fullPrompt.append("### LEARNING FROM SUCCESS (Use these patterns):\n")
                      .append(successfulExamples).append("\n\n");
        }
        
        if (failedExamples != null && !failedExamples.isEmpty()) {
            fullPrompt.append("### LEARNING FROM PAST ERRORS (AVOID THESE):\n")
                      .append(failedExamples).append("\n\n");
        }

        fullPrompt.append("### MISSION:\n")
                  .append(basePrompt)
                  .append("\n\nGUIDELINES:\n")
                  .append("- Use multi-stage builds to minimize image size.\n")
                  .append("- Always use a non-root user for security.\n")
                  .append("- Leverage layer caching by copying dependency files first.\n")
                  .append("- Add a HEALTHCHECK where possible.\n")
                  .append("- Keep it generic and adaptable for large projects.\n\n")
                  .append("Return ONLY the Dockerfile content. No explanation.");

        OllamaRequest request = new OllamaRequest();
        request.setModel(model);
        request.setPrompt(fullPrompt.toString());
        request.setStream(false);
        request.setOptions(Map.of("temperature", 0.2, "num_ctx", 8192));

        try {
            OllamaResponse response = restTemplate.postForObject(url, request, OllamaResponse.class);
            if (response == null || response.getResponse() == null) {
                throw new DockerfileGenerationException("Empty response from Ollama");
            }

            String dockerfile = response.getResponse().trim();
            validateDockerfile(dockerfile);
            return dockerfile;

        } catch (Exception e) {
            throw new DockerfileGenerationException("Failed to generate Dockerfile via Ollama: " + e.getMessage(), e);
        }
    }

    private void validateDockerfile(String content) {
        String upperContent = content.toUpperCase();
        boolean valid = upperContent.contains("FROM") &&
                        upperContent.contains("COPY") &&
                        (upperContent.contains("CMD") || upperContent.contains("ENTRYPOINT"));

        if (!valid) {
            throw new DockerfileGenerationException("Generated Dockerfile failed validation constraints.");
        }
    }

    @Data
    private static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
        private Map<String, Object> options;
    }

    @Data
    private static class OllamaResponse {
        private String response;
    }
}
