package com.jean202.cardmizer.api.spending

import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.DeleteSpendingRecordUseCase
import com.jean202.cardmizer.core.port.`in`.GetSpendingRecordsUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class SpendingRecordControllerGetDeleteTest {
    private val kst = ZoneId.of("Asia/Seoul")
    private val fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), kst)
    private val normalizer = TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault())

    @Test
    fun getWithYearMonthReturnsWrappedResponse() {
        val recordId = UUID.randomUUID()
        val record = SpendingRecord(recordId, CardId("SAMSUNG"), Money.won(50_000), LocalDate.of(2026, 3, 15), "테스트가맹점", "TEST")
        val getUseCase = CapturingGetUseCase(listOf(record))
        val controller = controller(getUseCase, NoOpDeleteUseCase())

        val response = controller.list("2026-03", null)

        assertEquals(1, response.count)
        assertEquals("2026-03", response.yearMonth)
        assertEquals(recordId, response.records[0].id)
        assertEquals(YearMonth.of(2026, 3), getUseCase.capturedPeriod!!.yearMonth)
        assertNull(getUseCase.capturedCardId)
    }

    @Test
    fun getWithYearMonthAndCardIdPassesCardIdToUseCase() {
        val getUseCase = CapturingGetUseCase(emptyList())
        val controller = controller(getUseCase, NoOpDeleteUseCase())

        controller.list("2026-03", "SAMSUNG_KPASS")

        assertEquals(CardId("SAMSUNG_KPASS"), getUseCase.capturedCardId)
    }

    @Test
    fun getWithoutParamsDefaultsToCurrentKstMonth() {
        val getUseCase = CapturingGetUseCase(emptyList())
        val controller = controller(getUseCase, NoOpDeleteUseCase())

        val response = controller.list(null, null)

        assertEquals(YearMonth.of(2026, 3), getUseCase.capturedPeriod!!.yearMonth)
        assertEquals("2026-03", response.yearMonth)
    }

    @Test
    fun deleteCallsUseCaseWithCorrectUuid() {
        val deleteUseCase = CapturingDeleteUseCase()
        val controller = controller(CapturingGetUseCase(emptyList()), deleteUseCase)
        val targetId = UUID.randomUUID()

        controller.delete(targetId)

        assertEquals(targetId, deleteUseCase.deletedId)
    }

    private fun controller(getUseCase: GetSpendingRecordsUseCase, deleteUseCase: DeleteSpendingRecordUseCase) =
        SpendingRecordController({ }, getUseCase, deleteUseCase, normalizer, fixedClock)

    private class CapturingGetUseCase(private val records: List<SpendingRecord>) : GetSpendingRecordsUseCase {
        var capturedPeriod: SpendingPeriod? = null
        var capturedCardId: CardId? = null

        override fun getByPeriod(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord> {
            capturedPeriod = period
            capturedCardId = cardId
            return records
        }
    }

    private class CapturingDeleteUseCase : DeleteSpendingRecordUseCase {
        var deletedId: UUID? = null
        override fun delete(id: UUID) { deletedId = id }
    }

    private class NoOpDeleteUseCase : DeleteSpendingRecordUseCase {
        override fun delete(id: UUID) {}
    }
}
