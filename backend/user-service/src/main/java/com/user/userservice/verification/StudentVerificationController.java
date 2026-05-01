package com.user.userservice.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la vérification de carte étudiant via IA (Ollama LLaVA).
 */
@RestController
@RequestMapping("/api/users/student-verification")
@Tag(name = "Student Verification", description = "Vérification de carte étudiant par IA (Ollama LLaVA)")
public class StudentVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(StudentVerificationController.class);

    private final StudentVerificationService verificationService;

    public StudentVerificationController(StudentVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/version")
    public String getVersion() {
        return "v2.6-RESIZE-ACTIVE";
    }

    // ─── Utilisateur ─────────────────────────────────────────────────────────

    /**
     * Upload multipart avec annotations Swagger correctes pour afficher le champ "file".
     * Le schéma @Schema + @Encoding force Swagger UI à afficher un vrai file picker.
     */
    @Operation(
        summary = "Uploader une carte étudiant pour vérification IA",
        description = "Upload une image JPG/PNG/PDF. L'IA Ollama (LLaVA) l'analyse automatiquement.",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(implementation = StudentVerificationController.UploadRequest.class),
                encoding = @Encoding(name = "file", contentType = "image/jpeg, image/png, application/pdf")
            )
        )
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentVerificationResponse> uploadStudentCard(
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam("userId") Long userId) {

        logger.info("Upload carte étudiant pour userId: {}", userId);
        try {
            StudentVerificationResponse response = verificationService.uploadAndVerify(userId, file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Fichier invalide: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de l'upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Schéma interne pour Swagger UI — représente le contenu du formulaire multipart.
     */
    @Schema(name = "UploadRequest", description = "Formulaire d'upload de carte étudiant")
    static class UploadRequest {
        @Schema(description = "Image de la carte étudiant (JPG, PNG, PDF)", type = "string", format = "binary", required = true)
        public MultipartFile file;

        @Schema(description = "ID de l'utilisateur", example = "1", required = true)
        public Long userId;
    }

    @Operation(summary = "Récupérer le statut de vérification d'un utilisateur")
    @GetMapping("/status/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentVerificationResponse> getVerificationStatus(@PathVariable Long userId) {
        return verificationService.getVerificationStatus(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @Operation(summary = "[ADMIN] Récupérer toutes les vérifications en attente de revue manuelle")
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentVerificationResponse>> getPendingReviews() {
        logger.info("Admin: récupération des vérifications en attente");
        return ResponseEntity.ok(verificationService.getPendingAdminReviews());
    }

    @Operation(summary = "[ADMIN] Approuver manuellement une vérification")
    @PostMapping("/admin/{verificationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentVerificationResponse> adminApprove(@PathVariable Long verificationId) {
        logger.info("Admin: approbation de la vérification {}", verificationId);
        return ResponseEntity.ok(verificationService.adminApprove(verificationId));
    }

    @Operation(summary = "[ADMIN] Rejeter manuellement une vérification")
    @PostMapping("/admin/{verificationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentVerificationResponse> adminReject(
            @PathVariable Long verificationId,
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Document invalide");
        logger.info("Admin: rejet de la vérification {} : {}", verificationId, reason);
        return ResponseEntity.ok(verificationService.adminReject(verificationId, reason));
    }
}
