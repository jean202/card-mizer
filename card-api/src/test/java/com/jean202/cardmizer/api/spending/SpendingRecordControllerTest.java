package com.jean202.cardmizer.api.spending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpendingRecordControllerTest {
    @Test
    void normalizesSpendingRecordRequestBeforeSaving() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        SpendingRecordController controller = new SpendingRecordController(useCase, new TransactionNormalizer());

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
