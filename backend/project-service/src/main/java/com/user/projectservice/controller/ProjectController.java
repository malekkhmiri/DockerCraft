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

    @PostMapping(value = "/submit", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un nouveau projet (ZIP)")
    public ResponseEntity<?> uploadProject(
            @RequestParam("name") String name,
            @RequestParam("language") String language,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("username") String username,
            @RequestPart("file") MultipartFile file) {
        try {
            System.out.println(">>> REQUEST RECEIVED: /api/projects/upload for " + name);
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("ERREUR : Le fichier ZIP est vide !");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(
                projectService.uploadProject(name, language, userEmail, username, file)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("ERREUR UPLOAD : " + e.getMessage() + " (Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "N/A") + ")");
        }
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
    
    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger l'archive du projet")
    public ResponseEntity<org.springframework.core.io.Resource> downloadProject(@PathVariable Long id) {
        try {
            String archivePath = projectService.getArchivePath(id);
            java.nio.file.Path path = java.nio.file.Paths.get(archivePath);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);
            } else {
                throw new RuntimeException("Fichier non trouvé ou illisible");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du téléchargement : " + e.getMessage());
        }
    }
}
