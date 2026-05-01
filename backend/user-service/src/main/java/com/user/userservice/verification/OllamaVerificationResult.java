package com.user.userservice.verification;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO représentant la réponse JSON de l'analyse IA (Ollama/LLaVA).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaVerificationResult {

    @JsonProperty("studentName")
    @JsonAlias({"student_name", "full_name", "name"})
    private String studentName;

    @JsonProperty("studentId")
    @JsonAlias({"student_id", "matricule", "id_number"})
    private String studentId;

    @JsonProperty("university")
    @JsonAlias({"university", "school", "institution", "school_name", "university_name"})
    private String university;

    @JsonProperty("academicYear")
    @JsonAlias({"academic_year", "year", "session"})
    private String academicYear;

    @JsonProperty("isValidStudentCard")
    @JsonAlias({"valid", "is_valid", "isValid"})
    private boolean validStudentCard;

    @JsonProperty("currentYearValid")
    @JsonAlias({"current_year_valid", "is_current_year"})
    private boolean currentYearValid;

    @JsonProperty("suspiciousSigns")
    private List<String> suspiciousSigns;

    @JsonProperty("confidenceScore")
    @JsonAlias({"confidence_score", "confidence", "score"})
    private double confidenceScore;

    @JsonProperty("rejectionReason")
    private String rejectionReason;

    // ─── Constructors ────────────────────────────────────────────────────────
    public OllamaVerificationResult() {}

    // ─── Getters & Setters ───────────────────────────────────────────────────
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public boolean isValidStudentCard() { return validStudentCard; }
    public void setValidStudentCard(boolean validStudentCard) { this.validStudentCard = validStudentCard; }

    public boolean isCurrentYearValid() { return currentYearValid; }
    public void setCurrentYearValid(boolean currentYearValid) { this.currentYearValid = currentYearValid; }

    public List<String> getSuspiciousSigns() { return suspiciousSigns; }
    public void setSuspiciousSigns(List<String> suspiciousSigns) { this.suspiciousSigns = suspiciousSigns; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    @Override
    public String toString() {
        return "OllamaVerificationResult{" +
               "studentName='" + studentName + '\'' +
               ", studentId='" + studentId + '\'' +
               ", university='" + university + '\'' +
               ", academicYear='" + academicYear + '\'' +
               ", validStudentCard=" + validStudentCard +
               ", confidenceScore=" + confidenceScore +
               '}';
    }
}
