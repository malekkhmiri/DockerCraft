package com.user.dockerfileservice.service;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@org.springframework.context.annotation.Lazy
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final RestTemplate restTemplate;

    @Value("${ollama.url:http://dc-ollama:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5-coder:3b}")
    private String modelName;

    public LLMService(@org.springframework.beans.factory.annotation.Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generate(String prompt) {
        String systemPrompt = "You are a Senior DevOps Engineer. Generate an optimal, production-ready, multi-stage Dockerfile. " +
                "Ensure small image size, security best practices (non-root user), and efficient caching. " +
                "Return ONLY the Dockerfile content without any explanation.\n\n";
        
        logger.info("Appel d'Ollama ({}) avec le modèle : {}", ollamaUrl, modelName);
        
        Map<String, Object> request = Map.of(
            "model", modelName,
            "prompt", systemPrompt + prompt,
            "stream", false
        );

        try {
            OllamaResponse response = restTemplate.postForObject(ollamaUrl + "/api/generate", request, OllamaResponse.class);
            if (response != null && response.getResponse() != null) {
                return cleanResponse(response.getResponse());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à Ollama", e);
        }

        return "# Erreur lors de la génération par l'IA\nFROM alpine\nCMD [\"echo\", \"error\"]";
    }

    private String cleanResponse(String response) {
        // 1. Enlever les balises markdown
        String cleaned = response.replaceAll("(?s)```(?:dockerfile)?(.*?)```", "$1");
        
        // 2. Extraire uniquement du premier FROM jusqu'à la fin de la dernière instruction Docker probable
        // (Cela évite les bavardages de l'IA à la fin)
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?s)(FROM\\s+.*)").matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group(1);
        }

        return cleaned.trim();
    }

    @Data
    private static class OllamaResponse {
        private String response;
    }
}
