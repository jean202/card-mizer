package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import java.util.List;
import java.util.Objects;

public interface GetPerformanceOverviewUseCase {
    List<CardPerformanceSnapshot> getOverview(SpendingPeriod period);

    record CardPerformanceSnapshot(
            Card card,
            Money spentAmount,
            Money targetAmount,
            Money remainingAmount,
            boolean achieved,
            String targetTierCode
    ) {
        public CardPerformanceSnapshot {
            Objects.requireNonNull(card, "card must not be null");
            Objects.requireNonNull(spentAmount, "spentAmount must not be null");
            Objects.requireNonNull(targetAmount, "targetAmount must not be null");
            Objects.requireNonNull(remainingAmount, "remainingAmount must not be null");
            if (targetTierCode == null || targetTierCode.isBlank()) {
                throw new IllegalArgumentException("targetTierCode must not be blank");
            }
        }
    }
}
