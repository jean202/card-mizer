package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class GetSpendingRecordsServiceTest {
    private val march2026 = SpendingPeriod(YearMonth.of(2026, 3))
    private val samsung = CardId("SAMSUNG_KPASS")
    private val hyundai = CardId("HYUNDAI_ZERO")

    @Test
    fun returnsRecordsForPeriod() {
        val marchRecord = record(samsung, LocalDate.of(2026, 3, 15), 50_000)
        val port = fakePort(listOf(marchRecord))
        val service = GetSpendingRecordsService(port)

        val result = service.getByPeriod(march2026, null)

        assertEquals(1, result.size)
        assertEquals(marchRecord, result[0])
    }

    @Test
    fun withCardIdFilterReturnsOnlyMatchingRecords() {
        val samsungRecord = record(samsung, LocalDate.of(2026, 3, 10), 30_000)
        val hyundaiRecord = record(hyundai, LocalDate.of(2026, 3, 12), 20_000)
        val port = fakePort(listOf(samsungRecord, hyundaiRecord))
        val service = GetSpendingRecordsService(port)

        val result = service.getByPeriod(march2026, samsung)

        assertEquals(1, result.size)
        assertEquals(samsung, result[0].cardId)
    }

    @Test
    fun withoutCardIdReturnsAllCardsRecords() {
        val samsungRecord = record(samsung, LocalDate.of(2026, 3, 10), 30_000)
        val hyundaiRecord = record(hyundai, LocalDate.of(2026, 3, 12), 20_000)
        val port = fakePort(listOf(samsungRecord, hyundaiRecord))
        val service = GetSpendingRecordsService(port)

        val result = service.getByPeriod(march2026, null)

        assertEquals(2, result.size)
    }

    @Test
    fun emptyPeriodReturnsEmptyList() {
        val port = fakePort(emptyList())
        val service = GetSpendingRecordsService(port)

        val result = service.getByPeriod(march2026, null)

        assertTrue(result.isEmpty())
    }

    @Test
    fun sortsBySpentOnDescThenIdDesc() {
        val id1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val id2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val id3 = UUID.fromString("00000000-0000-0000-0000-000000000003")

        val early1 = SpendingRecord(id1, samsung, Money.won(10_000), LocalDate.of(2026, 3, 5), "A", "GROCERY")
        val early2 = SpendingRecord(id2, samsung, Money.won(20_000), LocalDate.of(2026, 3, 5), "B", "GROCERY")
        val late = SpendingRecord(id3, samsung, Money.won(30_000), LocalDate.of(2026, 3, 20), "C", "GROCERY")

        val port = fakePort(listOf(early1, early2, late))
        val service = GetSpendingRecordsService(port)

        val result = service.getByPeriod(march2026, null)

        assertEquals(3, result.size)
        assertEquals(id3, result[0].id)
        assertEquals(id2, result[1].id)
        assertEquals(id1, result[2].id)
    }

    private fun record(cardId: CardId, spentOn: LocalDate, amount: Long) =
        SpendingRecord(UUID.randomUUID(), cardId, Money.won(amount), spentOn, "테스트가맹점", "TEST")

    private fun fakePort(allRecords: List<SpendingRecord>): LoadSpendingRecordsByCardAndPeriodPort =
        LoadSpendingRecordsByCardAndPeriodPort { period, cardId ->
            allRecords
                .filter { period.includes(it.spentOn) }
                .filter { cardId == null || it.cardId == cardId }
                .sortedWith(compareByDescending<SpendingRecord> { it.spentOn }.thenByDescending { it.id })
        }
}
