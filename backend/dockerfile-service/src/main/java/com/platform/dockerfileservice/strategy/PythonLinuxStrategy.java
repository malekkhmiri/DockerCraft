package com.platform.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class PythonLinuxStrategy implements LanguageStrategy {

    @Override
    public String getSupportedLanguage() {
        return "python";
    }

    @Override
    public String generatePrompt(AnalysisResult analysis) {
        return "You are a Senior DevOps Engineer. Generate a Premium, production-ready Dockerfile for a Python project.\n" +
               "Details:\n" +
               "- Framework: " + (analysis.getFramework() != null ? analysis.getFramework() : "Python") + "\n" +
               "- Port: " + analysis.getPort() + "\n" +
               "- OS: Linux Slim\n\n" +
               "Best Practices:\n" +
               "1. Use 'python:3.11-slim' as base.\n" +
               "2. Security: Create and use a non-root user.\n" +
               "3. Environment: Set PYTHONUNBUFFERED=1 and PYTHONDONTWRITEBYTECODE=1.\n" +
               "4. Optimization: Use a virtualenv or install dependencies in a single layer with clean up.\n" +
               "5. Observability: Add a HEALTHCHECK.\n\n" +
               "Output ONLY the Dockerfile. No markdown. No explanation.";
    }
}
