package com.user.userservice.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Service d'analyse de carte étudiant combinant OCR (Tess4J) et LLM (Gemma 2).
 * Optimisé avec OpenCV pour un prétraitement rapide et précis.
 */
@Service
public class OllamaCardAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaCardAnalysisService.class);

    static {
        OpenCV.loadShared();
        logger.info("[OpenCV] Bibliothèque native chargée avec succès.");
    }

    @Value("${tesseract.datapath:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tessDataPath;

    private final OllamaChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OllamaCardAnalysisService(OllamaChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = "ollamaService", fallbackMethod = "analyzeFallback")
    @Retry(name = "ollamaService")
    public StudentCardAnalysisResult analyzeStudentCard(byte[] imageBytes) {
        logger.info("[Pipeline] Début de l'analyse (OCR + OpenCV + LLM)...");

        // 1. Prétraitement de l'image avec OpenCV
        byte[] processedImageBytes = preprocessImage(imageBytes);

        // 2. OCR (Extraction de texte)
        String rawText = performOCR(processedImageBytes);
        logger.info("[OCR] Texte extrait: {}", rawText.replaceAll("\\n", " "));

        if (rawText.isBlank()) {
            return buildFallbackResult("Impossible d'extraire du texte après prétraitement.");
        }

        // 3. Validation et Structuration avec LLM (Gemma 2)
        return validateWithLLM(rawText);
    }

    private byte[] preprocessImage(byte[] imageBytes) {
        long start = System.currentTimeMillis();
        Mat src = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (src.empty()) return imageBytes;

        Mat gray = new Mat();
        Mat resized = new Mat();
        Mat contrast = new Mat();
        Mat thresh = new Mat();

        // 1. Conversion en niveaux de gris
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 2. Redimensionnement (x2 pour une meilleure netteté des caractères)
        double scale = 2.0;
        Size size = new Size(gray.cols() * scale, gray.rows() * scale);
        Imgproc.resize(gray, resized, size, 0, 0, Imgproc.INTER_CUBIC);

        // 3. Augmentation du contraste adaptative (CLAHE)
        // Cela aide énormément à faire ressortir le texte sur des fonds colorés ou avec des reflets
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(resized, contrast);

        // 4. Réduction du bruit légère
        Mat blurred = new Mat();
        Imgproc.medianBlur(contrast, blurred, 3);

        // 5. Binarisation d'Otsu (souvent plus stable que l'adaptative pour les cartes)
        // On essaie de produire du texte noir sur fond blanc (Tesseract adore ça)
        Imgproc.threshold(blurred, thresh, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // 6. Optionnel: Inversion si le fond est sombre (on vérifie la moyenne)
        Scalar mean = Core.mean(thresh);
        if (mean.val[0] < 127) {
            Core.bitwise_not(thresh, thresh);
        }

        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".png", thresh, buf); // PNG est sans perte, mieux pour l'OCR
        
        logger.info("[OpenCV] Prétraitement optimisé terminé en {}ms", System.currentTimeMillis() - start);
        return buf.toArray();
    }

    private String performOCR(byte[] imageBytes) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("fra+eng+ara"); // Ajout du support de l'arabe
        
        tesseract.setPageSegMode(3);
        tesseract.setTessVariable("user_defined_dpi", "300");

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) return "";
            return tesseract.doOCR(image);
        } catch (TesseractException | IOException e) {
            logger.error("[OCR] Erreur critique: {}", e.getMessage());
            return "";
        }
    }

    private StudentCardAnalysisResult validateWithLLM(String rawText) {
        String promptText = String.format("""
            Tu es un extracteur de données JSON. Analyse le texte OCR d'une carte étudiant et extrait les infos.
            
            TEXTE OCR :
            \"\"\"
            %s
            \"\"\"
            
            CONSIGNES :
            - nom : extrait uniquement le nom de famille de l'étudiant (en MAJUSCULES).
            - prenom : extrait uniquement le prénom de l'étudiant (en MAJUSCULES).
            - numero_etudiant : extrait le matricule.
            - institution : extrait le nom de l'école ou université.
            - score : confiance (0.0 à 1.0).
            
            IMPORTANT : Ne recopie JAMAIS ces consignes dans les valeurs du JSON. Si tu ne trouves pas une info, mets "".
            
            FORMAT DE RÉPONSE (JSON UNIQUEMENT) :
            {
              "nom": "",
              "prenom": "",
              "numero_etudiant": "",
              "institution": "",
              "date_naissance": "",
              "score": 0.0,
              "valide": true,
              "erreurs": []
            }
            """, rawText);

        UserMessage userMessage = new UserMessage(promptText);
        OllamaOptions options = new OllamaOptions();
        options.setModel("gemma2:2b");
        options.setTemperature(0.0);

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(userMessage, options));
            String output = chatResponse.getResult().getOutput().getText();
            return parseLLMResponse(output);
        } catch (Exception e) {
            logger.error("[LLM] Erreur: {}", e.getMessage());
            return buildFallbackResult("Erreur de communication avec le modèle.");
        }
    }

    private StudentCardAnalysisResult parseLLMResponse(String rawResponse) {
        try {
            int start = rawResponse.indexOf('{');
            int end = rawResponse.lastIndexOf('}');
            if (start != -1 && end != -1) {
                String json = rawResponse.substring(start, end + 1);
                StudentCardAnalysisResult result = objectMapper.readValue(json, StudentCardAnalysisResult.class);
                return sanitizeResult(result);
            }
        } catch (Exception e) {
            logger.warn("[LLM] Erreur parsing: {}", e.getMessage());
        }
        return buildFallbackResult("Erreur de format IA.");
    }

    private StudentCardAnalysisResult sanitizeResult(StudentCardAnalysisResult result) {
        // Sécurité contre les hallucinations où l'IA recopie les instructions
        if (result.getNom() != null && result.getNom().contains("Cherche les mots")) {
            result.setNom("");
        }
        if (result.getPrenom() != null && result.getPrenom().contains("Cherche les mots")) {
            result.setPrenom("");
        }
        return result;
    }

    public StudentCardAnalysisResult analyzeFallback(byte[] imageBytes, Throwable t) {
        return buildFallbackResult("Service momentanément indisponible.");
    }

    private StudentCardAnalysisResult buildFallbackResult(String reason) {
        StudentCardAnalysisResult result = new StudentCardAnalysisResult();
        result.setValide(false);
        result.getErreurs().add(reason);
        return result;
    }

    public StudentVerification.VerificationStatus determineStatus(StudentCardAnalysisResult result) {
        if (result.isValide() && result.getNom() != null && !result.getNom().isEmpty()) {
            return StudentVerification.VerificationStatus.AI_APPROVED;
        }
        return StudentVerification.VerificationStatus.ADMIN_REVIEW;
    }
}
