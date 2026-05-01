package com.user.dockerfileservice.service;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorService.class);

    /** Minimal validation without project context (backward compatibility). */
    public boolean validate(String content) {
        return validate(content, null);
    }

    public boolean validate(String content, AnalysisResult a) {
        if (content == null || content.isBlank()) return false;
        if (a == null) return content.contains("FROM") && content.contains("ENTRYPOINT");

        List<String> errors = new ArrayList<>();

        // 1. Validation de l'image de build
        if (a.getMavenImageRecommended() != null && !content.contains(a.getMavenImageRecommended()))
            errors.add("Image Maven incorrecte. Attendu: " + a.getMavenImageRecommended());

        // 2. Validation de l'artifact JAR
        String expectedJar = a.getArtifactName() != null ? a.getArtifactName() : "app.jar";
        if (!content.contains(expectedJar) && !content.contains("*.jar"))
            errors.add("Nom du JAR incorrect. Attendu: " + expectedJar);

        // 3. Validation du port
        if (!content.contains("EXPOSE " + a.getPort()))
            errors.add("Port incorrect. Attendu: EXPOSE " + a.getPort());

        // 4. Validation des packages PostgreSQL
        if ("postgresql".equals(a.getDatabaseType()) && !content.contains("libpq"))
            errors.add("Package libpq manquant pour PostgreSQL");

        // 6. Validation du Healthcheck
        if (!content.contains("HEALTHCHECK"))
            errors.add("HEALTHCHECK absent");

        if (errors.isEmpty()) {
            logger.info("✅ Dockerfile validé sémantiquement.");
            return true;
        } else {
            errors.forEach(err -> logger.error("❌ Erreur validation : {}", err));
            return false;
        }
    }
}
