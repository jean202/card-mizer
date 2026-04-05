package com.jean202.cardmizer.api.spending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.DeleteSpendingRecordUseCase;
import com.jean202.cardmizer.core.port.in.GetSpendingRecordsUseCase;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpendingRecordControllerGetDeleteTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-15T12:00:00Z"), KST);

    @Test
    void getWithYearMonthReturnsWrappedResponse() {
        UUID recordId = UUID.randomUUID();
        SpendingRecord record = new SpendingRecord(
                recordId, new CardId("SAMSUNG"), Money.won(50_000),
                LocalDate.of(2026, 3, 15), "테스트가맹점", "TEST"
        );
        CapturingGetUseCase getUseCase = new CapturingGetUseCase(List.of(record));
        SpendingRecordController controller = controller(getUseCase, new NoOpDeleteUseCase());

        SpendingRecordController.SpendingRecordsResponse response = controller.list("2026-03", null);

        assertEquals(1, response.count());
        assertEquals("2026-03", response.yearMonth());
        assertEquals(recordId, response.records().get(0).id());
        assertEquals(YearMonth.of(2026, 3), getUseCase.capturedPeriod.yearMonth());
        assertNull(getUseCase.capturedCardId);
    }

    @Test
    void getWithYearMonthAndCardIdPassesCardIdToUseCase() {
        CapturingGetUseCase getUseCase = new CapturingGetUseCase(List.of());
        SpendingRecordController controller = controller(getUseCase, new NoOpDeleteUseCase());

        controller.list("2026-03", "SAMSUNG_KPASS");

        assertEquals(new CardId("SAMSUNG_KPASS"), getUseCase.capturedCardId);
    }

    @Test
    void getWithoutParamsDefaultsToCurrentKstMonth() {
        CapturingGetUseCase getUseCase = new CapturingGetUseCase(List.of());
        SpendingRecordController controller = controller(getUseCase, new NoOpDeleteUseCase());

        SpendingRecordController.SpendingRecordsResponse response = controller.list(null, null);

        assertEquals(YearMonth.of(2026, 3), getUseCase.capturedPeriod.yearMonth());
        assertEquals("2026-03", response.yearMonth());
    }

    @Test
    void deleteCallsUseCaseWithCorrectUuid() {
        CapturingDeleteUseCase deleteUseCase = new CapturingDeleteUseCase();
        SpendingRecordController controller = controller(
                new CapturingGetUseCase(List.of()), deleteUseCase);
        UUID targetId = UUID.randomUUID();

        controller.delete(targetId);

        assertEquals(targetId, deleteUseCase.deletedId);
    }

    private SpendingRecordController controller(
            GetSpendingRecordsUseCase getUseCase,
            DeleteSpendingRecordUseCase deleteUseCase
    ) {
        return new SpendingRecordController(
                spendingRecord -> {},
                getUseCase,
                deleteUseCase,
                new TransactionNormalizer(),
                FIXED_CLOCK
        );
    }

    private static final class CapturingGetUseCase implements GetSpendingRecordsUseCase {
        private final List<SpendingRecord> records;
        private SpendingPeriod capturedPeriod;
        private CardId capturedCardId;

        CapturingGetUseCase(List<SpendingRecord> records) {
            this.records = records;
        }

        @Override
        public List<SpendingRecord> getByPeriod(SpendingPeriod period, CardId cardId) {
            this.capturedPeriod = period;
            this.capturedCardId = cardId;
            return records;
        }
    }

    private static final class CapturingDeleteUseCase implements DeleteSpendingRecordUseCase {
        private UUID deletedId;

        @Override
        public void delete(UUID id) {
            this.deletedId = id;
        }
    }

    private static final class NoOpDeleteUseCase implements DeleteSpendingRecordUseCase {
        @Override
        public void delete(UUID id) {
        }
    }
}
