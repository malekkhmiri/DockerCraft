package com.user.dockerfileservice.service;

import com.user.dockerfileservice.entity.DockerfileKnowledge;
import com.user.dockerfileservice.repository.KnowledgeRepository;
import com.user.dockerfileservice.strategy.LanguageStrategy;
import com.user.dockerfileservice.strategy.StrategyRegistry;
import com.user.dockerfileservice.dto.AnalysisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerfileGenerationServiceTest {

    @Mock
    private StrategyRegistry strategyRegistry;

    @Mock
    private LLMService llmService;

    @Mock
    private FallbackTemplateService fallbackTemplateService;

    @Mock
    private LanguageStrategy languageStrategy;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private ValidatorService validatorService;

    @Mock
    private DockerfilePostProcessor postProcessor;

    @InjectMocks
    private DockerfileGenerationService generationService;

    @Test
    void testGenerateSuccess() {
        AnalysisResult analysis = AnalysisResult.builder()
                .language("java")
                .javaVersion("17")
                .port(8080)
                .build();

        when(strategyRegistry.getStrategy(anyString())).thenReturn(languageStrategy);
        when(languageStrategy.generatePrompt(any())).thenReturn("prompt");
        when(llmService.generate(anyString())).thenReturn("FROM openjdk:17");
        when(postProcessor.process(anyString(), any())).thenReturn("FROM openjdk:17");
        when(validatorService.validate(anyString(), any())).thenReturn(true);
        when(knowledgeRepository.findTop3ByLanguageAndStatusOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(List.of());

        String result = generationService.generate(analysis);

        assertNotNull(result);
        verify(llmService).generate(anyString());
    }

    @Test
    void testGenerateFallbackOnValidationFailure() {
        AnalysisResult analysis = AnalysisResult.builder()
                .language("java")
                .javaVersion("17")
                .port(8080)
                .build();

        when(strategyRegistry.getStrategy(anyString())).thenReturn(languageStrategy);
        when(languageStrategy.generatePrompt(any())).thenReturn("prompt");
        when(llmService.generate(anyString())).thenReturn("FROM openjdk:17");
        when(postProcessor.process(anyString(), any())).thenReturn("FROM openjdk:17");
        when(validatorService.validate(anyString(), any())).thenReturn(false);
        when(fallbackTemplateService.generateFallback(any())).thenReturn("FROM eclipse-temurin:17-jre-alpine");
        when(knowledgeRepository.findTop3ByLanguageAndStatusOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(List.of());

        String result = generationService.generate(analysis);

        assertNotNull(result);
        verify(fallbackTemplateService).generateFallback(any());
    }
}
