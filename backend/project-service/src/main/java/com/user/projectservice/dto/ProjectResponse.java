package com.user.projectservice.dto;

import com.user.projectservice.entity.ProjectLanguage;
import com.user.projectservice.entity.ProjectStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private java.time.LocalDateTime createdAt;
    
    private ProjectStatus status;
    private ProjectLanguage language;
    private String userEmail;
    private String username;
    private String archivePath;
    private String extractedPath;
    private String executionTime;
    private String reference;
}
