package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import java.util.List;

public interface GetSpendingRecordsUseCase {
    List<SpendingRecord> getByPeriod(SpendingPeriod period, CardId cardId);
}
