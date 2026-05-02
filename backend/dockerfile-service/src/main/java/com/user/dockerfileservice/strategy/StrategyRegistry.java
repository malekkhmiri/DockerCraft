package com.user.dockerfileservice.strategy;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StrategyRegistry {

    private final List<LanguageStrategy> strategies;

    public StrategyRegistry(List<LanguageStrategy> strategies) {
        this.strategies = strategies;
    }

    public LanguageStrategy getStrategy(String language) {
        return strategies.stream()
                .filter(s -> s.getSupportedLanguage().equalsIgnoreCase(language))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No strategy for " + language));
    }

    public LanguageStrategy find(String language, com.platform.dockerfileservice.model.ProjectContext.TargetOS os) {
        return strategies.stream()
                .filter(s -> s.getSupportedLanguage().equalsIgnoreCase(language) && s.supportsOS(os))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No strategy for " + language + " on " + os));
    }
}


