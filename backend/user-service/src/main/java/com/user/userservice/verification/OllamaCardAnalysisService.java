package com.user.userservice.verification;

import org.springframework.stereotype.Service;

@Service
public class OllamaCardAnalysisService {
    
    public StudentCardAnalysisResult analyzeStudentCard(byte[] imageBytes) {
        StudentCardAnalysisResult mockResult = new StudentCardAnalysisResult();
        mockResult.setNom("Analyse OCR Désactivée");
        mockResult.setScore(100.0);
        return mockResult;
    }
    
    public StudentVerification.VerificationStatus determineStatus(StudentCardAnalysisResult result) {
        // En mode sans OCR, on force la revue manuelle (Admin Review) ou on approuve.
        // Pour la simplicité du déploiement, on passe en revue manuelle.
        return StudentVerification.VerificationStatus.ADMIN_REVIEW;
    }
}
