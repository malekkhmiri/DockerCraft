package com.platform.dockerfileservice.service;

import com.platform.dockerfileservice.exception.DockerfileGenerationException;
import com.platform.dockerfileservice.model.ProjectContext;
import com.platform.dockerfileservice.strategy.LanguageStrategy;
import com.platform.dockerfileservice.strategy.StrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerfileGenerationServiceTest {

    @Mock
    private StrategyRegistry strategyRegistry;

    @Mock
    private OllamaService ollamaService;

    @Mock
    private FallbackTemplateService fallbackTemplateService;

    @Mock
    private LanguageStrategy languageStrategy;

    @Mock
    private com.platform.dockerfileservice.repository.KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private DockerfileGenerationService generationService;

    @Test
    void testOllamaSuccess() {
        ProjectContext ctx = ProjectContext.builder()
                .language("java")
                .targetOS(ProjectContext.TargetOS.LINUX)
                .build();

        when(strategyRegistry.find(any(), any())).thenReturn(languageStrategy);
        when(languageStrategy.buildPrompt(any())).thenReturn("prompt");
        when(ollamaService.generateDockerfile(any(), any(), any())).thenReturn("FROM ollama");

        String result = generationService.generate(ctx);

        assertEquals("FROM ollama", result);
        verify(ollamaService).generateDockerfile(any(), any(), any());
        verify(fallbackTemplateService, never()).generate(any());
    }

    @Test
    void testFallbackOnOllamaFailure() {
        ProjectContext ctx = ProjectContext.builder()
                .language("java")
                .targetOS(ProjectContext.TargetOS.LINUX)
                .build();

        when(strategyRegistry.find(any(), any())).thenReturn(languageStrategy);
        when(languageStrategy.buildPrompt(any())).thenReturn("prompt");
        when(ollamaService.generateDockerfile(any(), any(), any())).thenThrow(new DockerfileGenerationException("Failed"));
        when(fallbackTemplateService.generate(any())).thenReturn("FROM template");

        String result = generationService.generate(ctx);

        assertEquals("FROM template", result);
        verify(fallbackTemplateService).generate(any());
    }
}
