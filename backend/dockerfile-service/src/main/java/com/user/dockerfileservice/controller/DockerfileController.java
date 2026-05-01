package com.user.dockerfileservice.controller;

import com.user.dockerfileservice.entity.Dockerfile;
import com.user.dockerfileservice.service.DockerfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dockerfiles")
@Tag(name = "Dockerfile Service", description = "Gestion et génération des Dockerfiles")
public class DockerfileController {

    private final DockerfileService service;

    public DockerfileController(DockerfileService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister tous les Dockerfiles")
    public ResponseEntity<List<Dockerfile>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Récupérer le dernier Dockerfile d'un projet")
    public ResponseEntity<Dockerfile> getByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProjectId(projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un Dockerfile par son ID")
    public ResponseEntity<Dockerfile> getById(@PathVariable Long id) {
        Dockerfile df = service.getById(id);
        return df != null ? ResponseEntity.ok(df) : ResponseEntity.notFound().build();
    }

    @GetMapping("/project/{projectId}/history")
    @Operation(summary = "Récupérer l'historique complet des Dockerfiles d'un projet")
    public ResponseEntity<List<Dockerfile>> getHistoryByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getHistoryByProjectId(projectId));
    }

    // DTO interne pour recevoir le contenu JSON {"content":"..."}
    public static class ContentRequest {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour le contenu d'un Dockerfile")
    public ResponseEntity<Dockerfile> update(@PathVariable Long id, @RequestBody ContentRequest request) {
        return ResponseEntity.ok(service.updateDockerfile(id, request.getContent()));
    }

    @GetMapping("/count")
    @Operation(summary = "Récupérer le nombre total de Dockerfiles")
    public ResponseEntity<Long> getCount() {
        return ResponseEntity.ok(service.count());
    }

    @PostMapping("/project/{projectId}/generate")
    @Operation(summary = "Déclencher une (ré)génération manuelle via LLM")
    public ResponseEntity<Void> generate(@PathVariable Long projectId) {
        service.generateDockerfile(projectId);
        return ResponseEntity.ok().build();
    }
}
