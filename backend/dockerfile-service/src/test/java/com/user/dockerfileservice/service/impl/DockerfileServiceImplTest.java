package com.user.dockerfileservice.service.impl;

import com.user.dockerfileservice.entity.Dockerfile;
import com.user.dockerfileservice.repository.DockerfileRepository;
import com.user.dockerfileservice.service.LLMService;
import com.user.dockerfileservice.dto.AnalysisResult;
import com.user.dockerfileservice.dto.ProjectResponse;
import com.user.dockerfileservice.service.ProjectAnalysisService;
import com.user.dockerfileservice.service.ValidatorService;
import com.user.dockerfileservice.strategy.LanguageStrategy;
import com.user.dockerfileservice.strategy.StrategyRegistry;
import com.user.dockerfileservice.service.DockerfilePostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerfileServiceImplTest {

    @Mock
    private DockerfileRepository repository;

    @Mock
    private LLMService llmService;

    @Mock
    private ProjectAnalysisService analysisService;

    @Mock
    private ValidatorService validatorService;

    @Mock
    private DockerfilePostProcessor postProcessor;

    @Mock
    private StrategyRegistry strategyRegistry;

    @Mock
    private LanguageStrategy languageStrategy;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DockerfileServiceImpl dockerfileService;

    @Test
    void generateDockerfile_ShouldSaveAndNotify() {
        // Arrange
        Long projectId = 1L;
        String userEmail = "test@example.com";
        String archivePath = "/tmp/test.zip";
        
        ProjectResponse projectResponse = ProjectResponse.builder()
                .id(projectId)
                .userEmail(userEmail)
                .archivePath(archivePath)
                .build();
                
        AnalysisResult analysisResult = AnalysisResult.builder()
                .language("JAVA")
                .buildTool("MAVEN")
                .build();

        when(restTemplate.getForObject(anyString(), eq(ProjectResponse.class))).thenReturn(projectResponse);
        when(analysisService.analyze(archivePath)).thenReturn(analysisResult);
        when(strategyRegistry.getStrategy(anyString())).thenReturn(languageStrategy);
        when(languageStrategy.generatePrompt(any())).thenReturn("DOCKER PROMPT");
        when(llmService.generate(anyString())).thenReturn("FROM openjdk:17");
        when(postProcessor.process(anyString(), any())).thenReturn("FROM openjdk:17");
        when(validatorService.validate(anyString())).thenReturn(true);
        
        // Simuler le succès du quota
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok().build());

        // Act
        dockerfileService.generateDockerfile(projectId);

        // Assert
        verify(repository).save(any(Dockerfile.class));
    }

    @Test
    void getByProjectId_ShouldReturnLatestDockerfile() {
        // Arrange
        Long projectId = 1L;
        Dockerfile dockerfile = Dockerfile.builder().projectId(projectId).content("test").build();
        when(repository.findTopByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.of(dockerfile));

        // Act
        Dockerfile result = dockerfileService.getByProjectId(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
    }

    @Test
    void updateDockerfile_ShouldCreateNewVersion() {
        // Arrange
        Long id = 1L;
        Long projectId = 100L;
        String newContent = "NEW CONTENT";
        Dockerfile oldDockerfile = Dockerfile.builder().id(id).projectId(projectId).content("old").build();
        when(repository.findById(id)).thenReturn(Optional.of(oldDockerfile));
        
        when(repository.save(any(Dockerfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Dockerfile result = dockerfileService.updateDockerfile(id, newContent);

        // Assert
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        assertEquals(projectId, result.getProjectId());
        
        verify(repository).save(argThat(d -> 
            d.getContent().equals(newContent) && d.getProjectId().equals(projectId)
        ));
    }
}
