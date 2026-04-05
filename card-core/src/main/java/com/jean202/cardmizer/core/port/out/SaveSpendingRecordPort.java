package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.SpendingRecord;

public interface SaveSpendingRecordPort {
    void save(SpendingRecord spendingRecord);
}
