package com.user.userservice.verification;

import com.user.userservice.EmailProducer;
import com.user.userservice.User;
import com.user.userservice.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(StudentVerificationService.class);
    private static final String UPLOAD_DIR = "uploads/student-cards/";
    private static final int VERIFICATION_VALIDITY_MONTHS = 12;

    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final OllamaCardAnalysisService ollamaService;
    private final EmailProducer emailProducer;

    public StudentVerificationService(
            StudentVerificationRepository verificationRepository,
            UserRepository userRepository,
            OllamaCardAnalysisService ollamaService,
            EmailProducer emailProducer) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.ollamaService = ollamaService;
        this.emailProducer = emailProducer;
    }

    // ─── Upload & Vérification ───────────────────────────────────────────────

    @Transactional
    public StudentVerificationResponse uploadAndVerify(Long userId, MultipartFile file) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

        validateImageFile(file);
        String imagePath = saveImage(file, userId);

        StudentVerification verification = verificationRepository.findByUserId(userId)
                .orElse(new StudentVerification(user));

        verification.setCardImagePath(imagePath);
        verification.setStatus(StudentVerification.VerificationStatus.AI_PROCESSING);
        verification.setUpdatedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        logger.info("Image uploadée pour l'utilisateur {} → lancement analyse Ollama", userId);

        byte[] imageBytes = file.getBytes();
        StudentCardAnalysisResult aiResult = ollamaService.analyzeStudentCard(imageBytes);

        verification.setExtractedName(aiResult.getNom() + " " + aiResult.getPrenom());
        verification.setExtractedStudentId(aiResult.getNumero_etudiant());
        verification.setExtractedDateOfBirth(aiResult.getDate_naissance());
        verification.setExtractedUniversity(aiResult.getInstitution());
        verification.setConfidenceScore(aiResult.getScore());
        verification.setAiAnalysis(String.join(", ", aiResult.getErreurs()));

        StudentVerification.VerificationStatus finalStatus = ollamaService.determineStatus(aiResult);

        boolean isDuplicate = false;
        String reasonDuplicate = null;

        if (aiResult.getNom() != null && !aiResult.getNom().isBlank()) {
            isDuplicate = verificationRepository.existsByStudentIdOrNameAndOtherUser(
                    aiResult.getNumero_etudiant(),
                    aiResult.getNom(),
                    userId);

            if (isDuplicate) {
                logger.warn("Tentative d'utilisation d'une carte étudiante en double ! User: {}, Name: {}, ID: {}",
                        userId, aiResult.getNom(), aiResult.getNumero_etudiant());
                finalStatus = StudentVerification.VerificationStatus.REJECTED;
                reasonDuplicate = "Cette carte étudiante (" + aiResult.getNom()
                        + ") a déjà été validée sur un autre compte.";
                verification.setRejectionReason(reasonDuplicate);
                verification.setAiAnalysis("BLOQUÉ: " + reasonDuplicate);
            }
        }

        verification.setStatus(finalStatus);

        if (finalStatus == StudentVerification.VerificationStatus.AI_APPROVED) {
            verification.setVerifiedAt(LocalDate.now());
            verification.setExpiresAt(LocalDate.now().plusMonths(VERIFICATION_VALIDITY_MONTHS));
            logger.info("✅ Utilisateur {} approuvé automatiquement par IA (Gemma 2)", userId);
        } else if (finalStatus == StudentVerification.VerificationStatus.ADMIN_REVIEW) {
            logger.info("👨‍💼 Utilisateur {} envoyé en revue admin", userId);
            notifyAdminForReview(user, verification);
        } else if (finalStatus == StudentVerification.VerificationStatus.REJECTED) {
            logger.info("❌ Utilisateur {} rejeté par IA", userId);
        }

        verification.setUpdatedAt(LocalDateTime.now());
        verificationRepository.save(verification);
        notifyUser(user, finalStatus, aiResult);

        return toResponse(verification);
    }

    // ─── Admin Actions ───────────────────────────────────────────────────────

    @Transactional
    public StudentVerificationResponse adminApprove(Long verificationId) {
        StudentVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Vérification non trouvée: " + verificationId));

        verification.setStatus(StudentVerification.VerificationStatus.ADMIN_APPROVED);
        verification.setVerifiedAt(LocalDate.now());
        verification.setExpiresAt(LocalDate.now().plusMonths(VERIFICATION_VALIDITY_MONTHS));
        verification.setUpdatedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        logger.info("✅ Admin a approuvé la vérification {}", verificationId);
        notifyUser(verification.getUser(), StudentVerification.VerificationStatus.ADMIN_APPROVED, null);

        return toResponse(verification);
    }

    @Transactional
    public StudentVerificationResponse adminReject(Long verificationId, String reason) {
        StudentVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Vérification non trouvée: " + verificationId));

        verification.setStatus(StudentVerification.VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        verification.setUpdatedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        logger.info("❌ Admin a rejeté la vérification {} : {}", verificationId, reason);
        notifyUser(verification.getUser(), StudentVerification.VerificationStatus.REJECTED, null);

        return toResponse(verification);
    }

    public List<StudentVerificationResponse> getPendingAdminReviews() {
        return verificationRepository.findByStatus(StudentVerification.VerificationStatus.ADMIN_REVIEW)
                .stream().map(this::toResponse).toList();
    }

    public Optional<StudentVerificationResponse> getVerificationStatus(Long userId) {
        return verificationRepository.findByUserId(userId).map(this::toResponse);
    }

    // ─── Scheduled Jobs ──────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 1 * *")
    @Transactional
    public void checkExpiredVerifications() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(30);

        logger.info("🔄 Job de re-vérification annuelle démarré");

        List<StudentVerification> expired = verificationRepository.findExpiredVerifications(today);
        for (StudentVerification v : expired) {
            v.setStatus(StudentVerification.VerificationStatus.EXPIRED);
            v.setUpdatedAt(LocalDateTime.now());
            verificationRepository.save(v);
            sendExpirationEmail(v.getUser(), true);
            // ✅ CORRIGÉ : getEmail() au lieu de getEmail() sur l'objet User
            logger.info("⚠️ Vérification expirée pour l'utilisateur {}", v.getUser().getEmail());
        }

        List<StudentVerification> expiringSoon = verificationRepository
                .findVerificationsExpiringSoon(today, warningDate);
        for (StudentVerification v : expiringSoon) {
            sendExpirationEmail(v.getUser(), false);
            logger.info("📧 Rappel envoyé à l'utilisateur {} (expire le {})",
                    v.getUser().getEmail(), v.getExpiresAt());
        }

        logger.info("✅ Job terminé: {} expirées, {} rappels envoyés", expired.size(), expiringSoon.size());
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Format de fichier non supporté. Utilisez JPG, PNG ou PDF.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Le fichier ne doit pas dépasser 5 MB");
        }
    }

    private String saveImage(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = "user_" + userId + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());
        return filePath.toString();
    }

    private void notifyUser(User user, StudentVerification.VerificationStatus status,
                            StudentCardAnalysisResult aiResult) {
        try {
            String subject;
            String body;

            switch (status) {
                case AI_APPROVED, ADMIN_APPROVED -> {
                    subject = "✅ Votre vérification étudiante est approuvée - DockerGen AI";
                    body = String.format("""
                            Bonjour %s,

                            Votre statut d'étudiant a été vérifié avec succès !
                            Vous avez maintenant accès à toutes les fonctionnalités de DockerGen AI.

                            Votre vérification est valide jusqu'au %s.
                            Vous recevrez un rappel 30 jours avant l'expiration.

                            Bonne création DockerGen AI !
                            """,
                            // ✅ CORRIGÉ : getDisplayName() au lieu de getUsername()
                            user.getDisplayName(),
                            LocalDate.now().plusMonths(12));
                }
                case ADMIN_REVIEW -> {
                    subject = "⏳ Votre carte étudiant est en cours de vérification - DockerGen AI";
                    body = String.format("""
                            Bonjour %s,

                            Votre carte étudiant a été reçue et est en cours de vérification manuelle.
                            Vous recevrez une réponse sous 24-48 heures.

                            Merci de votre patience.
                            """,
                            // ✅ CORRIGÉ : getDisplayName() au lieu de getUsername()
                            user.getDisplayName());
                }
                case REJECTED -> {
                    subject = "❌ Vérification étudiante refusée - DockerGen AI";
                    body = String.format("""
                            Bonjour %s,

                            Votre vérification étudiante n'a pas pu être validée.
                            %s

                            Vous pouvez soumettre un nouveau document depuis votre profil.
                            Si vous pensez que c'est une erreur, contactez notre support.
                            """,
                            // ✅ CORRIGÉ : getDisplayName() au lieu de getUsername()
                            user.getDisplayName(),
                            aiResult != null && !aiResult.getErreurs().isEmpty()
                                    ? "Détails: " + String.join(", ", aiResult.getErreurs())
                                    : "Document invalide ou année académique incorrecte.");
                }
                default -> { return; }
            }

            emailProducer.queueSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            logger.error("Erreur envoi email notification à {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void notifyAdminForReview(User user, StudentVerification verification) {
        try {
            emailProducer.queueSimpleEmail(
                    "your-it-support@domain.com",
                    "👨‍💼 Nouvelle carte à réviser - DockerGen AI",
                    String.format("""
                            Un utilisateur nécessite une vérification manuelle:

                            Utilisateur: %s (%s)
                            Score IA: %.2f
                            Université extraite: %s
                            Année extraite: %s

                            Connectez-vous au dashboard admin pour approuver ou rejeter.
                            """,
                            // ✅ CORRIGÉ : getDisplayName() et getEmail()
                            user.getDisplayName(),
                            user.getEmail(),
                            verification.getConfidenceScore(),
                            verification.getExtractedUniversity(),
                            verification.getExtractedAcademicYear()));
        } catch (Exception e) {
            logger.error("Erreur envoi email admin: {}", e.getMessage());
        }
    }

    private void sendExpirationEmail(User user, boolean expired) {
        try {
            String subject = expired
                    ? "⚠️ Votre accès étudiant a expiré - DockerGen AI"
                    : "📅 Votre vérification étudiante expire bientôt - DockerGen AI";

            String body = expired
                    ? String.format(
                    "Bonjour %s,\n\nVotre vérification étudiante a expiré. Veuillez re-uploader votre carte étudiant.",
                    // ✅ CORRIGÉ : getDisplayName() au lieu de getUsername()
                    user.getDisplayName())
                    : String.format(
                    "Bonjour %s,\n\nVotre vérification étudiante expire dans 30 jours. Pensez à re-uploader votre nouvelle carte étudiant.",
                    // ✅ CORRIGÉ : getDisplayName() au lieu de getUsername()
                    user.getDisplayName());

            emailProducer.queueSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            logger.error("Erreur envoi email expiration: {}", e.getMessage());
        }
    }

    private StudentVerificationResponse toResponse(StudentVerification v) {
        if (v == null) return null;
        return new StudentVerificationResponse(
                v.getId(),
                // ✅ CORRIGÉ : getId() est maintenant explicite dans User
                v.getUser() != null ? v.getUser().getId() : null,
                v.getStatus() != null ? v.getStatus().name() : "PENDING",
                v.getConfidenceScore(),
                v.getExtractedName(),
                v.getExtractedStudentId(),
                v.getExtractedUniversity(),
                v.getExtractedAcademicYear(),
                v.getRejectionReason(),
                v.getVerifiedAt(),
                v.getExpiresAt(),
                v.getCreatedAt(),
                v.getAiAnalysis());
    }
}