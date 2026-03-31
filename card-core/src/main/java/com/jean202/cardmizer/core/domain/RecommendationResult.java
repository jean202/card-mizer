package com.jean202.cardmizer.core.domain;

import java.util.List;
import java.util.Objects;

public record RecommendationResult(
        Card recommendedCard,
        String reason,
        List<RecommendationCandidate> alternatives
) {
    public RecommendationResult {
        Objects.requireNonNull(recommendedCard, "recommendedCard must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be blank");
        }
        alternatives = List.copyOf(Objects.requireNonNull(alternatives, "alternatives must not be null"));
    }
}
