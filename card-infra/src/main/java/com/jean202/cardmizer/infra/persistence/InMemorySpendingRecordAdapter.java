package com.jean202.cardmizer.infra.persistence;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySpendingRecordAdapter implements LoadSpendingRecordsPort, SaveSpendingRecordPort {
    private final CopyOnWriteArrayList<SpendingRecord> spendingRecords = new CopyOnWriteArrayList<>();

    public InMemorySpendingRecordAdapter() {
    }

    public InMemorySpendingRecordAdapter(List<SpendingRecord> initialSpendingRecords) {
        spendingRecords.addAll(initialSpendingRecords);
    }

    @Override
    public List<SpendingRecord> loadByPeriod(SpendingPeriod period) {
        return spendingRecords.stream()
                .filter(spendingRecord -> period.includes(spendingRecord.spentOn()))
                .sorted(Comparator.comparing(SpendingRecord::spentOn))
                .toList();
    }

    @Override
    public void save(SpendingRecord spendingRecord) {
        spendingRecords.add(spendingRecord);
    }
}
