package com.jean202.cardmizer.core.domain;

import java.util.Objects;

public record RecommendationCandidate(Card card, String reason, int score) {
    public RecommendationCandidate {
        Objects.requireNonNull(card, "card must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be blank");
        }
        if (score < 0) {
            throw new IllegalArgumentException("Score must not be negative");
        }
    }
}
