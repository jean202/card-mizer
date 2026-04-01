package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import java.util.List;
import java.util.Objects;

public interface SyncCardTransactionsUseCase {
    SyncResult sync(SpendingPeriod period);

    record SyncResult(int fetchedCount, int savedCount, List<String> syncedCardIds) {
        public SyncResult {
            if (fetchedCount < 0) {
                throw new IllegalArgumentException("fetchedCount must not be negative");
            }
            if (savedCount < 0) {
                throw new IllegalArgumentException("savedCount must not be negative");
            }
            Objects.requireNonNull(syncedCardIds, "syncedCardIds must not be null");
        }
    }
}
