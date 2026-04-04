package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetSpendingRecordsServiceTest {
    private final SpendingPeriod march2026 = new SpendingPeriod(YearMonth.of(2026, 3));
    private final CardId samsung = new CardId("SAMSUNG_KPASS");
    private final CardId hyundai = new CardId("HYUNDAI_ZERO");

    @Test
    void returnsRecordsForPeriod() {
        SpendingRecord marchRecord = record(samsung, LocalDate.of(2026, 3, 15), 50_000);
        LoadSpendingRecordsByCardAndPeriodPort port = fakePort(List.of(marchRecord));
        GetSpendingRecordsService service = new GetSpendingRecordsService(port);

        List<SpendingRecord> result = service.getByPeriod(march2026, null);

        assertEquals(1, result.size());
        assertEquals(marchRecord, result.get(0));
    }

    @Test
    void withCardIdFilterReturnsOnlyMatchingRecords() {
        SpendingRecord samsungRecord = record(samsung, LocalDate.of(2026, 3, 10), 30_000);
        SpendingRecord hyundaiRecord = record(hyundai, LocalDate.of(2026, 3, 12), 20_000);
        LoadSpendingRecordsByCardAndPeriodPort port = fakePort(List.of(samsungRecord, hyundaiRecord));
        GetSpendingRecordsService service = new GetSpendingRecordsService(port);

        List<SpendingRecord> result = service.getByPeriod(march2026, samsung);

        assertEquals(1, result.size());
        assertEquals(samsung, result.get(0).cardId());
    }

    @Test
    void withoutCardIdReturnsAllCardsRecords() {
        SpendingRecord samsungRecord = record(samsung, LocalDate.of(2026, 3, 10), 30_000);
        SpendingRecord hyundaiRecord = record(hyundai, LocalDate.of(2026, 3, 12), 20_000);
        LoadSpendingRecordsByCardAndPeriodPort port = fakePort(List.of(samsungRecord, hyundaiRecord));
        GetSpendingRecordsService service = new GetSpendingRecordsService(port);

        List<SpendingRecord> result = service.getByPeriod(march2026, null);

        assertEquals(2, result.size());
    }

    @Test
    void emptyPeriodReturnsEmptyList() {
        LoadSpendingRecordsByCardAndPeriodPort port = fakePort(List.of());
        GetSpendingRecordsService service = new GetSpendingRecordsService(port);

        List<SpendingRecord> result = service.getByPeriod(march2026, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void sortsBySpentOnDescThenIdDesc() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID id3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        SpendingRecord early1 = new SpendingRecord(id1, samsung, Money.won(10_000), LocalDate.of(2026, 3, 5), "A", "GROCERY");
        SpendingRecord early2 = new SpendingRecord(id2, samsung, Money.won(20_000), LocalDate.of(2026, 3, 5), "B", "GROCERY");
        SpendingRecord late = new SpendingRecord(id3, samsung, Money.won(30_000), LocalDate.of(2026, 3, 20), "C", "GROCERY");

        LoadSpendingRecordsByCardAndPeriodPort port = fakePort(List.of(early1, early2, late));
        GetSpendingRecordsService service = new GetSpendingRecordsService(port);

        List<SpendingRecord> result = service.getByPeriod(march2026, null);

        assertEquals(3, result.size());
        assertEquals(id3, result.get(0).id());
        assertEquals(id2, result.get(1).id());
        assertEquals(id1, result.get(2).id());
    }

    private SpendingRecord record(CardId cardId, LocalDate spentOn, long amount) {
        return new SpendingRecord(UUID.randomUUID(), cardId, Money.won(amount), spentOn, "테스트가맹점", "TEST");
    }

    private LoadSpendingRecordsByCardAndPeriodPort fakePort(List<SpendingRecord> allRecords) {
        return (period, cardId) -> allRecords.stream()
                .filter(r -> period.includes(r.spentOn()))
                .filter(r -> cardId == null || r.cardId().equals(cardId))
                .sorted(Comparator.comparing(SpendingRecord::spentOn).reversed()
                        .thenComparing(Comparator.comparing(SpendingRecord::id).reversed()))
                .toList();
    }
}
