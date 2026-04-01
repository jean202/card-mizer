package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase.SyncResult;
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyncCardTransactionsServiceTest {
    @Test
    void fetchesAndSavesTransactionsForAllCards() {
        Card samsung = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스", CardType.CREDIT, 1);
        Card kb = new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2", CardType.CHECK, 2);
        SpendingPeriod march = new SpendingPeriod(YearMonth.of(2026, 3));

        SpendingRecord samsungTxn = txn("SAMSUNG_KPASS", 1250, 3, "서울교통공사", "대중교통");
        SpendingRecord kbTxn1 = txn("KB_NORI2_KBPAY", 5500, 5, "스타벅스 강남점", "카페");
        SpendingRecord kbTxn2 = txn("KB_NORI2_KBPAY", 14000, 10, "CGV 왕십리", "영화");

        FetchCardTransactionsPort fetchPort = (cardId, period) -> {
            if ("SAMSUNG_KPASS".equals(cardId.value())) {
                return List.of(samsungTxn);
            }
            if ("KB_NORI2_KBPAY".equals(cardId.value())) {
                return List.of(kbTxn1, kbTxn2);
            }
            return List.of();
        };

        CapturingSavePort savePort = new CapturingSavePort();

        SyncCardTransactionsService service = new SyncCardTransactionsService(
                () -> List.of(samsung, kb),
                fetchPort,
                savePort
        );

        SyncResult result = service.sync(march);

        assertAll(
                () -> assertEquals(3, result.fetchedCount()),
                () -> assertEquals(3, result.savedCount()),
                () -> assertEquals(List.of("SAMSUNG_KPASS", "KB_NORI2_KBPAY"), result.syncedCardIds()),
                () -> assertEquals(3, savePort.saved.size()),
                () -> assertEquals("서울교통공사", savePort.saved.get(0).merchantName()),
                () -> assertEquals("스타벅스 강남점", savePort.saved.get(1).merchantName()),
                () -> assertEquals("CGV 왕십리", savePort.saved.get(2).merchantName())
        );
    }

    @Test
    void returnsZeroCountsWhenNoCardsExist() {
        SyncCardTransactionsService service = new SyncCardTransactionsService(
                List::of,
                (cardId, period) -> List.of(),
                record -> {}
        );

        SyncResult result = service.sync(new SpendingPeriod(YearMonth.of(2026, 3)));

        assertAll(
                () -> assertEquals(0, result.fetchedCount()),
                () -> assertEquals(0, result.savedCount()),
                () -> assertEquals(0, result.syncedCardIds().size())
        );
    }

    @Test
    void rejectsNullPeriod() {
        SyncCardTransactionsService service = new SyncCardTransactionsService(
                List::of,
                (cardId, period) -> List.of(),
                record -> {}
        );

        assertThrows(NullPointerException.class, () -> service.sync(null));
    }

    private static SpendingRecord txn(String cardId, long amount, int day, String merchant, String category) {
        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId(cardId),
                Money.won(amount),
                LocalDate.of(2026, 3, day),
                merchant,
                category
        );
    }

    private static final class CapturingSavePort implements SaveSpendingRecordPort {
        private final List<SpendingRecord> saved = new ArrayList<>();

        @Override
        public void save(SpendingRecord spendingRecord) {
            saved.add(spendingRecord);
        }
    }
}
