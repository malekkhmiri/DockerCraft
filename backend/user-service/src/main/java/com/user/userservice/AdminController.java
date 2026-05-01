package com.user.userservice;

import com.user.userservice.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Controller", description = "Statistiques et gestion globale pour l'administrateur")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final ActivityService activityService;

    @Operation(summary = "Récupérer les statistiques globales")
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        log.info("Fetching admin stats via WebClient...");
        long projects = fetchDataCount("http://project-service/api/projects/count");
        long dockerfiles = fetchDataCount("http://dockerfile-service/api/dockerfiles/count");
        long pipelines = fetchDataCount("http://pipeline-service/api/pipelines/count");
        long images = fetchDataCount("http://image-service/api/images/count");

        return ResponseEntity.ok(AdminStatsDTO.builder()
                .totalUsers(userRepository.count())
                .totalProjects(projects)
                .generatedDockerfiles(dockerfiles)
                .totalPipelines(pipelines)
                .totalImages(images)
                .build());
    }

    private long fetchDataCount(String uri) {
        try {
            Long count = webClientBuilder.build().get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Long.class)
                    .block();
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Error fetching count from {}: {}", uri, e.getMessage());
            return 0L;
        }
    }

    @GetMapping("/pipelines/stats")
    public ResponseEntity<PipelineStatsDTO> getPipelineStats() {
        try {
            java.util.Map<String, Integer> stats = webClientBuilder.build().get()
                    .uri("http://pipeline-service/api/pipelines/stats")
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();

            if (stats != null) {
                return ResponseEntity.ok(PipelineStatsDTO.builder()
                        .success(stats.get("SUCCESS") != null ? ((Number) stats.get("SUCCESS")).intValue() : 0)
                        .inProgress(stats.get("RUNNING") != null ? ((Number) stats.get("RUNNING")).intValue() : 0)
                        .failed(stats.get("FAILED") != null ? ((Number) stats.get("FAILED")).intValue() : 0)
                        .build());
            }
        } catch (Exception e) {
            log.error("Erreur stats pipeline via WebClient : {}", e.getMessage());
        }
        return ResponseEntity.ok(PipelineStatsDTO.builder().success(0).inProgress(0).failed(0).build());
    }

    @GetMapping("/projects/recent")
    public ResponseEntity<List<RecentProjectDTO>> getRecentProjects() {
        try {
            ProjectResponseDTO[] projects = webClientBuilder.build().get()
                    .uri("http://project-service/api/projects")
                    .retrieve()
                    .bodyToMono(ProjectResponseDTO[].class)
                    .block();

            if (projects == null) return ResponseEntity.ok(new ArrayList<>());
            
            List<RecentProjectDTO> dtos = java.util.Arrays.stream(projects)
                    .map(p -> RecentProjectDTO.builder()
                            .id(p.getId().toString())
                            .name(p.getName())
                            .user(p.getUsername())
                            .status(p.getStatus())
                            .uploadDate(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "")
                            .language(p.getLanguage())
                            .build())
                    .limit(8)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur projets récents via WebClient : {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/pipelines/recent")
    public ResponseEntity<List<PipelineDTO>> getRecentPipelines() {
        try {
            ProjectResponseDTO[] projects = webClientBuilder.build().get()
                    .uri("http://project-service/api/projects")
                    .retrieve()
                    .bodyToMono(ProjectResponseDTO[].class)
                    .block();

            java.util.Map<Long, String> projectNames = projects != null ?
                    java.util.Arrays.stream(projects).collect(Collectors.toMap(ProjectResponseDTO::getId, ProjectResponseDTO::getName, (a, b) -> a)) :
                    new java.util.HashMap<>();

            java.util.Map[] pipelines = webClientBuilder.build().get()
                    .uri("http://pipeline-service/api/pipelines")
                    .retrieve()
                    .bodyToMono(java.util.Map[].class)
                    .block();

            if (pipelines == null) return ResponseEntity.ok(new ArrayList<>());

            List<PipelineDTO> dtos = java.util.Arrays.stream(pipelines)
                    .map(p -> {
                        Long pid = p.get("projectId") != null ? Long.valueOf(p.get("projectId").toString()) : 0L;
                        return PipelineDTO.builder()
                                .id(p.get("id").toString())
                                .projectName(projectNames.getOrDefault(pid, "Projet #" + pid))
                                .status(p.get("status").toString())
                                .duration("30s")
                                .date(p.get("createdAt") != null ? p.get("createdAt").toString() : "")
                                .build();
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur pipelines récents via WebClient : {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/images")
    public ResponseEntity<List<DockerImageDTO>> getDockerImages() {
        try {
            ProjectResponseDTO[] projects = webClientBuilder.build().get()
                    .uri("http://project-service/api/projects")
                    .retrieve()
                    .bodyToMono(ProjectResponseDTO[].class)
                    .block();

            java.util.Map<Long, String> projectNames = projects != null ?
                    java.util.Arrays.stream(projects).collect(Collectors.toMap(ProjectResponseDTO::getId, ProjectResponseDTO::getName, (a, b) -> a)) :
                    new java.util.HashMap<>();

            java.util.Map[] images = webClientBuilder.build().get()
                    .uri("http://image-service/api/images")
                    .retrieve()
                    .bodyToMono(java.util.Map[].class)
                    .block();

            if (images == null) return ResponseEntity.ok(new ArrayList<>());

            List<DockerImageDTO> dtos = java.util.Arrays.stream(images)
                    .map(img -> {
                        Long pid = img.get("projectId") != null ? Long.valueOf(img.get("projectId").toString()) : 0L;
                        return DockerImageDTO.builder()
                                .name(img.get("imageName") != null ? img.get("imageName").toString() : "N/A")
                                .tag(img.get("tag") != null ? img.get("tag").toString() : "latest")
                                .projectName(projectNames.getOrDefault(pid, "Projet #" + pid))
                                .registryUrl("local-registry:5000")
                                .build();
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur images récents via WebClient : {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/activities")
    public ResponseEntity<List<SystemActivityDTO>> getActivities() {
        try {
            List<SystemActivityDTO> dtos = activityService.getRecentActivities().stream()
                    .map(a -> SystemActivityDTO.builder()
                            .type(a.getType())
                            .message(a.getMessage())
                            .user(a.getUser())
                            .timestamp(a.getTimestamp() != null ? a.getTimestamp().toString() : "")
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Activities error: {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
