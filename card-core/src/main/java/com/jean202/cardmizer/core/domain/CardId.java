package com.jean202.cardmizer.core.domain;

public record CardId(String value) {
    public CardId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Card id must not be blank");
        }
    }
}
