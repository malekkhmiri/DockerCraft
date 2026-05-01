package com.user.projectservice.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectLanguageConverter implements AttributeConverter<ProjectLanguage, String> {

    @Override
    public String convertToDatabaseColumn(ProjectLanguage attribute) {
        if (attribute == null) return null;
        return attribute.name();
    }

    @Override
    public ProjectLanguage convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            String val = dbData.toUpperCase().trim()
                .replace(".", "")
                .replace("-", "")
                .replace(" ", "");
            
            if (val.contains("JAVA")) return ProjectLanguage.JAVA;
            if (val.contains("NODE")) return ProjectLanguage.NODEJS;
            
            return ProjectLanguage.valueOf(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
