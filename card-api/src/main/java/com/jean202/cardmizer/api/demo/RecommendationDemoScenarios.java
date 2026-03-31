package com.jean202.cardmizer.api.demo;

import java.util.List;
import java.util.Objects;

public record RecommendationDemoScenarios(List<RecommendationDemoScenario> scenarios) {
    public RecommendationDemoScenarios {
        scenarios = List.copyOf(Objects.requireNonNull(scenarios, "scenarios must not be null"));
    }
}
