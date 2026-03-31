package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.util.Objects;

public class RecordSpendingService implements RecordSpendingUseCase {
    private final SaveSpendingRecordPort saveSpendingRecordPort;

    public RecordSpendingService(SaveSpendingRecordPort saveSpendingRecordPort) {
        this.saveSpendingRecordPort = Objects.requireNonNull(
                saveSpendingRecordPort,
                "saveSpendingRecordPort must not be null"
        );
    }

    @Override
    public void record(SpendingRecord spendingRecord) {
        Objects.requireNonNull(spendingRecord, "spendingRecord must not be null");
        saveSpendingRecordPort.save(spendingRecord);
    }
}
