package com.user.projectservice.service.impl;

import com.user.projectservice.config.RabbitMQConfig;
import com.user.projectservice.dto.ProjectResponse;
import com.user.projectservice.entity.Project;
import com.user.projectservice.entity.ProjectLanguage;
import com.user.projectservice.entity.ProjectStatus;
import com.user.projectservice.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.user.projectservice.dto.ProjectUploadedEvent;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(projectService, "uploadDir", tempDir.toString());
    }

    @Test
    void uploadProject_ShouldSaveProjectAndSendNotification() {
        // Arrange
        String name = "Test Project";
        String languageStr = "JAVA";
        ProjectLanguage language = ProjectLanguage.JAVA;
        String userEmail = "test@example.com";
        String username = "testuser";
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", "fake content".getBytes());
        Project savedProject = Project.builder()
                .id(1L)
                .name(name)
                .language(language)
                .userEmail(userEmail)
                .username(username)
                .status(ProjectStatus.UPLOADED)
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // Act
        ProjectResponse response = projectService.uploadProject(name, languageStr, userEmail, username, file);

        // Assert
        assertNotNull(response);
        assertEquals(name, response.getName());
        assertEquals(1L, response.getId());
        assertEquals(ProjectStatus.UPLOADED, response.getStatus());
        
        verify(projectRepository).save(any(Project.class));
        
        ProjectUploadedEvent expectedEvent = ProjectUploadedEvent.builder()
                .projectId(1L)
                .userEmail(userEmail)
                .name(name)
                .language("JAVA")
                .build();
                
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PROJECT_EXCHANGE),
                eq(RabbitMQConfig.PROJECT_UPLOADED_ROUTING_KEY),
                eq(expectedEvent)
        );
    }

    @Test
    void getProjectById_ShouldReturnProject() {
        // Arrange
        Long projectId = 1L;
        Project project = Project.builder()
                .id(projectId)
                .name("Project 1")
                .status(ProjectStatus.SUCCESS)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        ProjectResponse response = projectService.getProjectById(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(projectId, response.getId());
        assertEquals(ProjectStatus.SUCCESS, response.getStatus());
    }

    @Test
    void getProjectById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> projectService.getProjectById(1L));
    }
}
