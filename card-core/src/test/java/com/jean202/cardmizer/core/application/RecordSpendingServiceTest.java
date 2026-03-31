package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordSpendingServiceTest {
    @Test
    void savesSpendingRecordThroughPort() {
        CapturingSaveSpendingRecordPort saveSpendingRecordPort = new CapturingSaveSpendingRecordPort();
        RecordSpendingService service = new RecordSpendingService(saveSpendingRecordPort);

        SpendingRecord spendingRecord = new SpendingRecord(
                UUID.randomUUID(),
                new CardId("KB_CREDIT_MAIN"),
                Money.won(55_000),
                LocalDate.of(2026, 3, 28),
                "이마트",
                "GROCERY"
        );

        service.record(spendingRecord);

        assertNotNull(saveSpendingRecordPort.saved);
        assertEquals(spendingRecord, saveSpendingRecordPort.saved);
    }

    private static final class CapturingSaveSpendingRecordPort implements SaveSpendingRecordPort {
        private SpendingRecord saved;

        @Override
        public void save(SpendingRecord spendingRecord) {
            this.saved = spendingRecord;
        }
    }
}
