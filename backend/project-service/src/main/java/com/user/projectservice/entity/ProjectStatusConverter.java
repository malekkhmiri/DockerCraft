package com.user.projectservice.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectStatusConverter implements AttributeConverter<ProjectStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProjectStatus attribute) {
        if (attribute == null) return null;
        return attribute.name();
    }

    @Override
    public ProjectStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            // Conversion robuste pour les statuts
            String val = dbData.toUpperCase().trim().replace(" ", "_");
            
            // Gestion de BUILDING/RUNNING vers IN_PROGRESS pour la compatibilité ascendante
            if (val.equals("BUILDING") || val.equals("RUNNING")) {
                return ProjectStatus.IN_PROGRESS;
            }
            
            return ProjectStatus.valueOf(val);
        } catch (IllegalArgumentException e) {
            // Loguer ici s'il y a un statut inconnu ? Retourner UPLOADED par défaut.
            return ProjectStatus.UPLOADED;
        }
    }
}
