package com.user.dockerfileservice.strategy;

import com.user.dockerfileservice.dto.AnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class PythonWindowsStrategy implements LanguageStrategy {

    @Override
    public String getSupportedLanguage() {
        return "python";
    }

    @Override
    public String generatePrompt(AnalysisResult analysis) {
        return "Generate a Windows Dockerfile for a Python project. " +
               "Expose port " + analysis.getPort() + ".";
    }
}
