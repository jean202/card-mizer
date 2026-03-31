package com.jean202.cardmizer.api.demo;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public record DemoSpendingRecordFixture(
        String cardId,
        long amount,
        LocalDate spentOn,
        String merchantName,
        String merchantCategory,
        Set<String> paymentTags
) {
    public DemoSpendingRecordFixture {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        Objects.requireNonNull(spentOn, "spentOn must not be null");
        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException("merchantName must not be blank");
        }
        paymentTags = Set.copyOf(Objects.requireNonNull(paymentTags, "paymentTags must not be null"));
    }
}
