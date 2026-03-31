package com.jean202.cardmizer.core.domain;

import java.util.List;
import java.util.Objects;

public record PriorityStrategy(List<CardId> orderedCardIds) {
    public PriorityStrategy {
        orderedCardIds = List.copyOf(Objects.requireNonNull(orderedCardIds, "orderedCardIds must not be null"));
        if (orderedCardIds.isEmpty()) {
            throw new IllegalArgumentException("Priority strategy must contain at least one card");
        }
    }
}
