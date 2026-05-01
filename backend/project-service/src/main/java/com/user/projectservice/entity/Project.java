package com.user.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    @Convert(converter = ProjectStatusConverter.class)
    private ProjectStatus status;

    @Column(nullable = true)
    @Convert(converter = ProjectLanguageConverter.class)
    private ProjectLanguage language;
    
    private String userEmail;
    
    private String username;

    private String archivePath;
    private String extractedPath;
    
    private String executionTime;
    private String reference;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ProjectStatus.UPLOADED;
        }
    }
}
