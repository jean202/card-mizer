package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import java.util.List;

public interface FetchCardTransactionsPort {
    List<SpendingRecord> fetchByCardAndPeriod(CardId cardId, SpendingPeriod period);
}
