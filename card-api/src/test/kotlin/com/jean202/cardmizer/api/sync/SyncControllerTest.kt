package com.jean202.cardmizer.api.sync

import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase.SyncResult
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SyncControllerTest {
    @Test
    fun parsesYearMonthAndDelegatesToUseCase() {
        val useCase = CapturingSyncUseCase(SyncResult(5, 5, listOf("SAMSUNG_KPASS", "KB_NORI2_KBPAY")))
        val controller = SyncController(useCase)

        val response = controller.sync(SyncController.SyncRequest("2026-03"))

        assertAll(
            { assertEquals(YearMonth.of(2026, 3), useCase.capturedPeriod!!.yearMonth) },
            { assertEquals(5, response.fetchedCount) },
            { assertEquals(5, response.savedCount) },
            { assertEquals(listOf("SAMSUNG_KPASS", "KB_NORI2_KBPAY"), response.syncedCardIds) },
        )
    }

    private class CapturingSyncUseCase(private val fixedResult: SyncResult) : SyncCardTransactionsUseCase {
        var capturedPeriod: SpendingPeriod? = null

        override fun sync(period: SpendingPeriod): SyncResult {
            capturedPeriod = period
            return fixedResult
        }
    }
}
