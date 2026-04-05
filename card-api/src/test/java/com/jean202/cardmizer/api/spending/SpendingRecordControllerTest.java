package com.jean202.cardmizer.api.spending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpendingRecordControllerTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void normalizesSpendingRecordRequestBeforeSaving() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        SpendingRecordController controller = new SpendingRecordController(
                useCase, (period, cardId) -> List.of(), id -> {},
                new TransactionNormalizer(), FIXED_CLOCK);

        controller.create(new SpendingRecordController.CreateSpendingRecordRequest(
                "SAMSUNG_KPASS",
                9_900,
                LocalDate.of(2026, 3, 30),
                "넷플릭스",
                null,
                Set.of()
        ));

        assertEquals("OTT", useCase.saved.merchantCategory());
        assertTrue(useCase.saved.paymentTags().contains("SUBSCRIPTION"));
        assertTrue(useCase.saved.paymentTags().contains("ONLINE"));
    }

    private static final class CapturingRecordSpendingUseCase implements RecordSpendingUseCase {
        private SpendingRecord saved;

        @Override
        public void record(SpendingRecord spendingRecord) {
            this.saved = spendingRecord;
        }
    }
}
