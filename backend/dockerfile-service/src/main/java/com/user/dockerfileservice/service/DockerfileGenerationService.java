package com.user.dockerfileservice.service;

import com.user.dockerfileservice.entity.DockerfileKnowledge;
import com.user.dockerfileservice.repository.KnowledgeRepository;
import com.user.dockerfileservice.strategy.StrategyRegistry;
import com.user.dockerfileservice.dto.AnalysisResult;
import com.user.dockerfileservice.service.LLMService;
import com.user.dockerfileservice.service.ValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DockerfileGenerationService {

    private final StrategyRegistry strategyRegistry;
    private final LLMService llmService;
    private final KnowledgeRepository knowledgeRepository;
    private final FallbackTemplateService fallbackTemplateService;
    private final ValidatorService validatorService;
    private final com.user.dockerfileservice.service.DockerfilePostProcessor postProcessor;

    public DockerfileGenerationService(StrategyRegistry strategyRegistry,
                                     @org.springframework.context.annotation.Lazy LLMService llmService,
                                     KnowledgeRepository knowledgeRepository,
                                     FallbackTemplateService fallbackTemplateService,
                                     ValidatorService validatorService,
                                     com.user.dockerfileservice.service.DockerfilePostProcessor postProcessor) {
        this.strategyRegistry = strategyRegistry;
        this.llmService = llmService;
        this.knowledgeRepository = knowledgeRepository;
        this.fallbackTemplateService = fallbackTemplateService;
        this.validatorService = validatorService;
        this.postProcessor = postProcessor;
    }

    /**
     * Generation entry point using full AnalysisResult.
     */
    public String generate(AnalysisResult analysis) {
        log.info("Starting Intelligent Dockerfile generation for: {}", analysis.getLanguage());

        // 1. Fetch RAG Context
        List<DockerfileKnowledge> successes = knowledgeRepository.findTop3ByLanguageAndStatusOrderByCreatedAtDesc(
                analysis.getLanguage(), DockerfileKnowledge.Status.SUCCESS);
        List<DockerfileKnowledge> failures = knowledgeRepository.findTop3ByLanguageAndStatusOrderByCreatedAtDesc(
                analysis.getLanguage(), DockerfileKnowledge.Status.FAILURE);

        // 2. Generate Prompt (Adaptive Strategy)
        String basePrompt = strategyRegistry.getStrategy(analysis.getLanguage()).generatePrompt(analysis);
        String finalPrompt = enrichPromptWithKnowledge(basePrompt, successes, failures);

        // 3. LLM Generation & AUTO-CORRECTION
        String rawContent = llmService.generate(analysis, finalPrompt);
        String generatedContent = postProcessor.process(rawContent, analysis);

        // 4. Semantic Validation
        if (validatorService.validate(generatedContent, analysis)) {
            log.info("✅ LLM output validated successfully.");
            saveKnowledge(analysis, generatedContent, DockerfileKnowledge.Status.SUCCESS, null);
            return generatedContent;
        }

        // 5. Fallback (LLM cheated or hallucinated)
        log.warn("⚠️ LLM output failed validation. Triggering safe fallback.");
        String fallback = fallbackTemplateService.generateFallback(analysis);
        
        // On enregistre l'échec pour éviter de le reproduire
        saveKnowledge(analysis, generatedContent, DockerfileKnowledge.Status.FAILURE, "Validation failed");
        
        // CRITIQUE : On enregistre le Fallback comme un SUCCÈS pour que le RAG l'utilise comme exemple parfait la prochaine fois
        saveKnowledge(analysis, fallback, DockerfileKnowledge.Status.SUCCESS, "Fallback used as gold standard");
        
        return fallback;
    }

    private String enrichPromptWithKnowledge(String prompt, List<DockerfileKnowledge> ok, List<DockerfileKnowledge> fail) {
        StringBuilder sb = new StringBuilder(prompt);
        if (!ok.isEmpty()) {
            sb.append("\n\nSUCCESSFUL EXAMPLES (STRUCTURE ONLY - DO NOT COPY NAMES):\n");
            ok.forEach(k -> sb.append("---\n").append(sanitizeForExample(k.getGeneratedContent())).append("\n"));
        }
        if (!fail.isEmpty()) {
            sb.append("\n\nPREVIOUS FAILURES TO AVOID:\n");
            fail.forEach(k -> sb.append("---\n").append(k.getErrorMessage()).append("\n"));
        }
        return sb.toString();
    }

    /**
     * Replaces project-specific strings with placeholders to prevent LLM hallucinations.
     */
    private String sanitizeForExample(String content) {
        if (content == null) return "";
        return content
            .replaceAll("(?i)FoodFrenzy", "[PROJECT_NAME]")
            .replaceAll("(?i)jdbc:[a-z]+://[^/\\n]+/[a-z0-9_]+", "jdbc:protocol://db:port/[DATABASE_NAME]")
            .replaceAll("(?i)SPRING_DATASOURCE_URL=.*", "SPRING_DATASOURCE_URL=[SECURE_URL]");
    }

    private void saveKnowledge(AnalysisResult analysis, String content, DockerfileKnowledge.Status status, String error) {
        try {
            DockerfileKnowledge k = DockerfileKnowledge.builder()
                    .language(analysis.getLanguage())
                    .contextJson(analysis.toString())
                    .generatedContent(content)
                    .status(status)
                    .errorMessage(error)
                    .build();
            knowledgeRepository.save(k);
        } catch (Exception e) {
            log.error("Failed to save generation knowledge", e);
        }
    }
}
