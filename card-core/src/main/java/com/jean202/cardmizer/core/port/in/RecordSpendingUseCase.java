package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.SpendingRecord;

public interface RecordSpendingUseCase {
    void record(SpendingRecord spendingRecord);
}
