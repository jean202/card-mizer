package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.Objects;

public record BenefitMonthlyCapTier(Money minimumPreviousMonthSpent, Money monthlyCap) {
    public BenefitMonthlyCapTier {
        minimumPreviousMonthSpent = Objects.requireNonNull(
                minimumPreviousMonthSpent,
                "minimumPreviousMonthSpent must not be null"
        );
        monthlyCap = Objects.requireNonNull(monthlyCap, "monthlyCap must not be null");
    }
}
