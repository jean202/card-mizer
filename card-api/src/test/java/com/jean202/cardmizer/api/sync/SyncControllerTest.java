package com.jean202.cardmizer.api.sync;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncControllerTest {
    @Test
    void parsesYearMonthAndDelegatesToUseCase() {
        CapturingSyncUseCase useCase = new CapturingSyncUseCase(
                new SyncCardTransactionsUseCase.SyncResult(5, 5, List.of("SAMSUNG_KPASS", "KB_NORI2_KBPAY"))
        );
        SyncController controller = new SyncController(useCase);

        SyncController.SyncResponse response = controller.sync(
                new SyncController.SyncRequest("2026-03")
        );

        assertAll(
                () -> assertEquals(YearMonth.of(2026, 3), useCase.capturedPeriod.yearMonth()),
                () -> assertEquals(5, response.fetchedCount()),
                () -> assertEquals(5, response.savedCount()),
                () -> assertEquals(List.of("SAMSUNG_KPASS", "KB_NORI2_KBPAY"), response.syncedCardIds())
        );
    }

    private static final class CapturingSyncUseCase implements SyncCardTransactionsUseCase {
        private final SyncResult fixedResult;
        private SpendingPeriod capturedPeriod;

        CapturingSyncUseCase(SyncResult fixedResult) {
            this.fixedResult = fixedResult;
        }

        @Override
        public SyncResult sync(SpendingPeriod period) {
            this.capturedPeriod = period;
            return fixedResult;
        }
    }
}
