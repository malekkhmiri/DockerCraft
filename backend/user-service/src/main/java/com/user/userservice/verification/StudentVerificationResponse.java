package com.user.userservice.verification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour les vérifications étudiantes.
 */
public class StudentVerificationResponse {

    private Long id;
    private Long userId;
    private String status;
    private Double confidenceScore;
    private String extractedName;
    private String extractedStudentId;
    private String extractedUniversity;
    private String extractedAcademicYear;
    private String rejectionReason;
    private LocalDate verifiedAt;
    private LocalDate expiresAt;
    private LocalDateTime createdAt;
    private String aiAnalysis;

    public StudentVerificationResponse() {}

    public StudentVerificationResponse(Long id, Long userId, String status, Double confidenceScore,
                                       String extractedName, String extractedStudentId,
                                       String extractedUniversity, String extractedAcademicYear,
                                       String rejectionReason, LocalDate verifiedAt,
                                       LocalDate expiresAt, LocalDateTime createdAt, String aiAnalysis) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.confidenceScore = confidenceScore;
        this.extractedName = extractedName;
        this.extractedStudentId = extractedStudentId;
        this.extractedUniversity = extractedUniversity;
        this.extractedAcademicYear = extractedAcademicYear;
        this.rejectionReason = rejectionReason;
        this.verifiedAt = verifiedAt;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.aiAnalysis = aiAnalysis;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public Double getConfidenceScore() { return confidenceScore; }
    public String getExtractedName() { return extractedName; }
    public String getExtractedStudentId() { return extractedStudentId; }
    public String getExtractedUniversity() { return extractedUniversity; }
    public String getExtractedAcademicYear() { return extractedAcademicYear; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDate getVerifiedAt() { return verifiedAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAiAnalysis() { return aiAnalysis; }
}
