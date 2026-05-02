package com.user.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class NodeLinuxStrategy implements LanguageStrategy {

    @Override
    public String getSupportedLanguage() {
        return "nodejs";
    }

    @Override
    public String generatePrompt(AnalysisResult analysis) {
        return "You are a Senior DevOps Engineer. Generate a Premium, production-ready, multi-stage Dockerfile for a Node.js project.\n" +
               "Details:\n" +
               "- Framework: " + (analysis.getFramework() != null ? analysis.getFramework() : "Node.js") + "\n" +
               "- Port: " + analysis.getPort() + "\n" +
               "- OS: Linux Alpine\n\n" +
               "Best Practices:\n" +
               "1. Multi-stage build (stage 1: build, stage 2: production).\n" +
               "2. Use 'node:20-alpine' as base.\n" +
               "3. Layer caching: Copy package.json and lock file first.\n" +
               "4. Security: Run as 'node' user.\n" +
               "5. Environment: Set NODE_ENV=production.\n" +
               "6. Optimization: Use 'npm ci --only=production'.\n" +
               "7. Observability: Add a HEALTHCHECK.\n\n" +
               "Output ONLY the Dockerfile. No markdown. No explanation.";
    }
}
