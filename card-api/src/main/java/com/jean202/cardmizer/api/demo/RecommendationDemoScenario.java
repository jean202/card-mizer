package com.jean202.cardmizer.api.demo;

import java.util.List;
import java.util.Objects;

public record RecommendationDemoScenario(
        String id,
        String title,
        String description,
        String expectedRecommendedCardId,
        DemoRecommendationRequest request,
        List<DemoSpendingRecordFixture> seedRecords
) {
    public RecommendationDemoScenario {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (expectedRecommendedCardId == null || expectedRecommendedCardId.isBlank()) {
            throw new IllegalArgumentException("expectedRecommendedCardId must not be blank");
        }
        request = Objects.requireNonNull(request, "request must not be null");
        seedRecords = List.copyOf(Objects.requireNonNull(seedRecords, "seedRecords must not be null"));
    }
}
