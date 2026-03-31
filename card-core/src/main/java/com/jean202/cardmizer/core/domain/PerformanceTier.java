package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.Objects;

public record PerformanceTier(String code, Money targetAmount, String benefitSummary) {
    public PerformanceTier {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Tier code must not be blank");
        }
        targetAmount = Objects.requireNonNull(targetAmount, "targetAmount must not be null");
        if (benefitSummary == null || benefitSummary.isBlank()) {
            throw new IllegalArgumentException("Benefit summary must not be blank");
        }
    }
}
