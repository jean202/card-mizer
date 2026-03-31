package com.jean202.cardmizer.common;

import java.util.Objects;

public record Money(long amount) implements Comparable<Money> {
    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount must be zero or positive");
        }
    }

    public static Money won(long amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(Math.addExact(amount, other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        long result = Math.subtractExact(amount, other.amount);
        if (result < 0) {
            throw new IllegalArgumentException("Money amount must not become negative");
        }
        return new Money(result);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return amount >= other.amount;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return Long.compare(amount, other.amount);
    }
}
