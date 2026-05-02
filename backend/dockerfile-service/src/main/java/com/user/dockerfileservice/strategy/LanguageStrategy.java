package com.user.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;

/**
 * Contract for all language-specific Dockerfile prompt strategies.
 */
public interface LanguageStrategy {

    String getSupportedLanguage();

    String generatePrompt(AnalysisResult analysis);

    default boolean supportsOS(String os) {
        return "linux".equalsIgnoreCase(os);
    }
}
