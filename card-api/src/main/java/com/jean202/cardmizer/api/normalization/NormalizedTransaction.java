package com.jean202.cardmizer.api.normalization;

import java.util.Objects;
import java.util.Set;

public record NormalizedTransaction(String merchantCategory, Set<String> paymentTags) {
    public NormalizedTransaction {
        if (merchantCategory == null || merchantCategory.isBlank()) {
            throw new IllegalArgumentException("merchantCategory must not be blank");
        }
        paymentTags = Set.copyOf(Objects.requireNonNull(paymentTags, "paymentTags must not be null"));
    }
}
