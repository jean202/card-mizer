package com.jean202.cardmizer.api.normalization;

import java.util.Objects;
import java.util.Set;

public record MerchantNormalizationRule(String category, Set<String> keywords, Set<String> inferredTags) {
    public MerchantNormalizationRule {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        keywords = Set.copyOf(Objects.requireNonNull(keywords, "keywords must not be null"));
        inferredTags = Set.copyOf(Objects.requireNonNull(inferredTags, "inferredTags must not be null"));
        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("keywords must not be empty");
        }
    }
}
