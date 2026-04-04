package com.jean202.cardmizer.infra.persistence;

import com.jean202.cardmizer.core.application.ResourceNotFoundException;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySpendingRecordAdapter implements
        LoadSpendingRecordsPort,
        SaveSpendingRecordPort,
        LoadSpendingRecordsByCardAndPeriodPort,
        DeleteSpendingRecordPort {

    private final CopyOnWriteArrayList<SpendingRecord> spendingRecords = new CopyOnWriteArrayList<>();
    private final Set<UUID> deletedIds = new HashSet<>();

    public InMemorySpendingRecordAdapter() {
    }

    public InMemorySpendingRecordAdapter(List<SpendingRecord> initialSpendingRecords) {
        spendingRecords.addAll(initialSpendingRecords);
    }

    @Override
    public List<SpendingRecord> loadByPeriod(SpendingPeriod period) {
        return spendingRecords.stream()
                .filter(record -> !deletedIds.contains(record.id()))
                .filter(record -> period.includes(record.spentOn()))
                .sorted(Comparator.comparing(SpendingRecord::spentOn).thenComparing(SpendingRecord::id))
                .toList();
    }

    @Override
    public List<SpendingRecord> loadByPeriodAndCard(SpendingPeriod period, CardId cardId) {
        return spendingRecords.stream()
                .filter(record -> !deletedIds.contains(record.id()))
                .filter(record -> period.includes(record.spentOn()))
                .filter(record -> cardId == null || record.cardId().equals(cardId))
                .sorted(Comparator.comparing(SpendingRecord::spentOn).reversed()
                        .thenComparing(Comparator.comparing(SpendingRecord::id).reversed()))
                .toList();
    }

    @Override
    public void save(SpendingRecord spendingRecord) {
        spendingRecords.add(spendingRecord);
    }

    @Override
    public void delete(UUID id) {
        boolean exists = spendingRecords.stream().anyMatch(record -> record.id().equals(id));
        if (!exists || deletedIds.contains(id)) {
            throw new ResourceNotFoundException("Spending record not found: " + id);
        }
        deletedIds.add(id);
    }
}
