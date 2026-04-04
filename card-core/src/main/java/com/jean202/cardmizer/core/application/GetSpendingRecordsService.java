package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.GetSpendingRecordsUseCase;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort;
import java.util.List;
import java.util.Objects;

public class GetSpendingRecordsService implements GetSpendingRecordsUseCase {
    private final LoadSpendingRecordsByCardAndPeriodPort loadSpendingRecordsByCardAndPeriodPort;

    public GetSpendingRecordsService(LoadSpendingRecordsByCardAndPeriodPort loadSpendingRecordsByCardAndPeriodPort) {
        this.loadSpendingRecordsByCardAndPeriodPort = Objects.requireNonNull(
                loadSpendingRecordsByCardAndPeriodPort,
                "loadSpendingRecordsByCardAndPeriodPort must not be null"
        );
    }

    @Override
    public List<SpendingRecord> getByPeriod(SpendingPeriod period, CardId cardId) {
        Objects.requireNonNull(period, "period must not be null");
        return loadSpendingRecordsByCardAndPeriodPort.loadByPeriodAndCard(period, cardId);
    }
}
