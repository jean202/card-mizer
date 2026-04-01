package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase;
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SyncCardTransactionsService implements SyncCardTransactionsUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final FetchCardTransactionsPort fetchCardTransactionsPort;
    private final SaveSpendingRecordPort saveSpendingRecordPort;

    public SyncCardTransactionsService(
            LoadCardCatalogPort loadCardCatalogPort,
            FetchCardTransactionsPort fetchCardTransactionsPort,
            SaveSpendingRecordPort saveSpendingRecordPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.fetchCardTransactionsPort = Objects.requireNonNull(fetchCardTransactionsPort, "fetchCardTransactionsPort must not be null");
        this.saveSpendingRecordPort = Objects.requireNonNull(saveSpendingRecordPort, "saveSpendingRecordPort must not be null");
    }

    @Override
    public SyncResult sync(SpendingPeriod period) {
        Objects.requireNonNull(period, "period must not be null");

        List<Card> cards = loadCardCatalogPort.loadAll();
        int fetchedCount = 0;
        int savedCount = 0;
        List<String> syncedCardIds = new ArrayList<>();

        for (Card card : cards) {
            List<SpendingRecord> fetched = fetchCardTransactionsPort.fetchByCardAndPeriod(card.id(), period);
            fetchedCount += fetched.size();
            for (SpendingRecord record : fetched) {
                saveSpendingRecordPort.save(record);
                savedCount++;
            }
            syncedCardIds.add(card.id().value());
        }

        return new SyncResult(fetchedCount, savedCount, syncedCardIds);
    }
}
