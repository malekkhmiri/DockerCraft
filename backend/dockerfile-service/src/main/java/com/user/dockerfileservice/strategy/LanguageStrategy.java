package com.user.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;

/**
 * Contract for all language-specific Dockerfile prompt strategies.
 */
public interface LanguageStrategy {

    String getSupportedLanguage();

    String generatePrompt(AnalysisResult analysis);

    default boolean supportsOS(ProjectContext.TargetOS os) {
        return os == ProjectContext.TargetOS.LINUX;
    }

    /**
     * Bridge method: converts a ProjectContext (used in the generation pipeline)
     * into the richer AnalysisResult before delegating to generatePrompt().
     */
    default String buildPrompt(ProjectContext ctx) {
        return generatePrompt(AnalysisResult.builder()
                .language(ctx.getLanguage())
                .javaVersion(ctx.getVersion())
                .buildTool(ctx.getBuildTool())
                .port(ctx.getPort())
                .hasActuator(ctx.isHasActuator())
                .build());
    }
}
