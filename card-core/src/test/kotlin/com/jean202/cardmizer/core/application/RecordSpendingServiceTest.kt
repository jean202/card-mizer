package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RecordSpendingServiceTest {
    @Test
    fun savesSpendingRecordThroughPort() {
        val savePort = CapturingSaveSpendingRecordPort()
        val service = RecordSpendingService(savePort)

        val spendingRecord = SpendingRecord(
            UUID.randomUUID(),
            CardId("KB_CREDIT_MAIN"),
            Money.won(55_000),
            LocalDate.of(2026, 3, 28),
            "이마트",
            "GROCERY",
        )

        service.record(spendingRecord)

        assertNotNull(savePort.saved)
        assertEquals(spendingRecord, savePort.saved)
    }

    private class CapturingSaveSpendingRecordPort : SaveSpendingRecordPort {
        var saved: SpendingRecord? = null

        override fun save(spendingRecord: SpendingRecord) {
            saved = spendingRecord
        }
    }
}
