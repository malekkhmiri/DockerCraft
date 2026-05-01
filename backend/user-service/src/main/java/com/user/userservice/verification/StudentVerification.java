package com.user.userservice.verification;

import com.user.userservice.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_verifications")
public class StudentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "card_image_path")
    private String cardImagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "extracted_name")
    private String extractedName;

    @Column(name = "extracted_student_id")
    private String extractedStudentId;

    @Column(name = "extracted_university")
    private String extractedUniversity;

    @Column(name = "extracted_academic_year")
    private String extractedAcademicYear;

    @Column(name = "extracted_date_of_birth")
    private String extractedDateOfBirth;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "verified_at")
    private LocalDate verifiedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Enum ────────────────────────────────────────────────────────────────
    public enum VerificationStatus {
        PENDING,         // En attente (upload pas encore fait)
        AI_PROCESSING,   // En cours d'analyse par l'IA
        AI_APPROVED,     // Approuvé automatiquement par l'IA
        ADMIN_REVIEW,    // Envoyé à l'admin (score IA faible)
        ADMIN_APPROVED,  // Approuvé manuellement par l'admin
        REJECTED,        // Rejeté
        EXPIRED          // Expiré (re-vérification requise)
    }

    // ─── Constructors ────────────────────────────────────────────────────────
    public StudentVerification() {}

    public StudentVerification(User user) {
        this.user = user;
        this.status = VerificationStatus.PENDING;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCardImagePath() { return cardImagePath; }
    public void setCardImagePath(String cardImagePath) { this.cardImagePath = cardImagePath; }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getExtractedName() { return extractedName; }
    public void setExtractedName(String extractedName) { this.extractedName = extractedName; }

    public String getExtractedStudentId() { return extractedStudentId; }
    public void setExtractedStudentId(String extractedStudentId) { this.extractedStudentId = extractedStudentId; }

    public String getExtractedUniversity() { return extractedUniversity; }
    public void setExtractedUniversity(String extractedUniversity) { this.extractedUniversity = extractedUniversity; }

    public String getExtractedAcademicYear() { return extractedAcademicYear; }
    public void setExtractedAcademicYear(String extractedAcademicYear) { this.extractedAcademicYear = extractedAcademicYear; }

    public String getExtractedDateOfBirth() { return extractedDateOfBirth; }
    public void setExtractedDateOfBirth(String extractedDateOfBirth) { this.extractedDateOfBirth = extractedDateOfBirth; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public LocalDate getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDate verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ─── Helper ──────────────────────────────────────────────────────────────
    public boolean isApproved() {
        return status == VerificationStatus.AI_APPROVED || status == VerificationStatus.ADMIN_APPROVED;
    }

    public boolean isExpired() {
        return status == VerificationStatus.EXPIRED ||
               (expiresAt != null && LocalDate.now().isAfter(expiresAt));
    }
}
