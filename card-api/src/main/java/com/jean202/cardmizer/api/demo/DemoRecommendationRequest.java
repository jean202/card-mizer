package com.jean202.cardmizer.api.demo;

import java.util.Objects;
import java.util.Set;

public record DemoRecommendationRequest(
        String spendingMonth,
        long amount,
        String merchantName,
        String merchantCategory,
        Set<String> paymentTags
) {
    public DemoRecommendationRequest {
        if (spendingMonth == null || spendingMonth.isBlank()) {
            throw new IllegalArgumentException("spendingMonth must not be blank");
        }
        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException("merchantName must not be blank");
        }
        paymentTags = Set.copyOf(Objects.requireNonNull(paymentTags, "paymentTags must not be null"));
    }
}
