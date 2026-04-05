package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import java.util.List;

public interface LoadSpendingRecordsPort {
    List<SpendingRecord> loadByPeriod(SpendingPeriod period);
}
