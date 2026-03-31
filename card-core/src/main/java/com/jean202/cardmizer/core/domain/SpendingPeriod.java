package com.jean202.cardmizer.core.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public record SpendingPeriod(YearMonth yearMonth) {
    public SpendingPeriod {
        Objects.requireNonNull(yearMonth, "yearMonth must not be null");
    }

    public boolean includes(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return YearMonth.from(date).equals(yearMonth);
    }

    public SpendingPeriod previous() {
        return new SpendingPeriod(yearMonth.minusMonths(1));
    }

    public List<SpendingPeriod> fromStartOfYear() {
        return IntStream.rangeClosed(1, yearMonth.getMonthValue())
                .mapToObj(month -> new SpendingPeriod(YearMonth.of(yearMonth.getYear(), month)))
                .toList();
    }
}
