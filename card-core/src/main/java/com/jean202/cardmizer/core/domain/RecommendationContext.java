package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record RecommendationContext(
        SpendingPeriod spendingPeriod,
        Money amount,
        String merchantName,
        String merchantCategory,
        Set<String> paymentTags
) {
    public RecommendationContext(SpendingPeriod spendingPeriod, Money amount, String merchantName, String merchantCategory) {
        this(spendingPeriod, amount, merchantName, merchantCategory, Set.of());
    }

    public RecommendationContext {
        Objects.requireNonNull(spendingPeriod, "spendingPeriod must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException("Merchant name must not be blank");
        }
        merchantName = merchantName.trim();
        merchantCategory = normalizeValue(merchantCategory == null || merchantCategory.isBlank()
                ? "UNCATEGORIZED"
                : merchantCategory);
        paymentTags = normalizeValues(paymentTags);
    }

    private static String normalizeValue(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> normalizeValues(Set<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return Set.of();
        }
        Set<String> normalizedTags = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            if (rawTag != null && !rawTag.isBlank()) {
                normalizedTags.add(normalizeValue(rawTag));
            }
        }
        return Set.copyOf(normalizedTags);
    }
}
