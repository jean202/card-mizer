package com.jean202.cardmizer.core.domain;

import java.util.Objects;

public record Card(
        CardId id,
        String issuerName,
        String productName,
        CardType cardType,
        int priority
) {
    public Card {
        Objects.requireNonNull(id, "id must not be null");
        cardType = Objects.requireNonNull(cardType, "cardType must not be null");

        if (issuerName == null || issuerName.isBlank()) {
            throw new IllegalArgumentException("Issuer name must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (priority < 1) {
            throw new IllegalArgumentException("Priority must start from 1");
        }
    }

    public String displayName() {
        return issuerName + " " + productName;
    }
}
