package com.user.projectservice.controller;

import com.user.projectservice.dto.ProjectResponse;
import com.user.projectservice.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Service", description = "Gestion des projets et uploads")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un nouveau projet (ZIP)")
    public ResponseEntity<ProjectResponse> uploadProject(
            @RequestParam("name") String name,
            @RequestParam("language") String language,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("username") String username,
            @RequestPart("file") MultipartFile file) {
        System.out.println(">>> REQUEST RECEIVED: /api/projects/upload for " + name);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            projectService.uploadProject(name, language, userEmail, username, file)
        );
    }

    @GetMapping
    @Operation(summary = "Lister tous les projets")
    public ResponseEntity<?> getAllProjects(@RequestParam(required = false) String userEmail) {
        try {
            if (userEmail != null && !userEmail.isEmpty()) {
                return ResponseEntity.ok(projectService.getProjectsByUserEmail(userEmail));
            }
            return ResponseEntity.ok(projectService.getAllProjects());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("ERREUR BACKEND : " + e.getMessage() + " (Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "N/A") + ")");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les détails d'un projet")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un projet")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Récupérer le nombre total de projets")
    public ResponseEntity<Long> getProjectCount() {
        return ResponseEntity.ok(projectService.countProjects());
    }
    @PostMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'un projet")
    public ResponseEntity<Void> updateProjectStatus(@PathVariable Long id, @RequestParam String status) {
        projectService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
