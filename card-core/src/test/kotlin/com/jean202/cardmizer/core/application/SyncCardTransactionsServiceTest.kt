package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class SyncCardTransactionsServiceTest {
    @Test
    fun fetchesAndSavesTransactionsForAllCards() {
        val samsung = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스", CardType.CREDIT, 1)
        val kb = Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2", CardType.CHECK, 2)
        val march = SpendingPeriod(YearMonth.of(2026, 3))

        val samsungTxn = txn("SAMSUNG_KPASS", 1250, 3, "서울교통공사", "대중교통")
        val kbTxn1 = txn("KB_NORI2_KBPAY", 5500, 5, "스타벅스 강남점", "카페")
        val kbTxn2 = txn("KB_NORI2_KBPAY", 14000, 10, "CGV 왕십리", "영화")

        val fetchPort = FetchCardTransactionsPort { cardId, _ ->
            when (cardId.value) {
                "SAMSUNG_KPASS" -> listOf(samsungTxn)
                "KB_NORI2_KBPAY" -> listOf(kbTxn1, kbTxn2)
                else -> emptyList()
            }
        }
        val savePort = CapturingSavePort()
        val service = SyncCardTransactionsService({ listOf(samsung, kb) }, fetchPort, savePort)

        val result = service.sync(march)

        assertAll(
            { assertEquals(3, result.fetchedCount) },
            { assertEquals(3, result.savedCount) },
            { assertEquals(listOf("SAMSUNG_KPASS", "KB_NORI2_KBPAY"), result.syncedCardIds) },
            { assertEquals(3, savePort.saved.size) },
            { assertEquals("서울교통공사", savePort.saved[0].merchantName) },
            { assertEquals("스타벅스 강남점", savePort.saved[1].merchantName) },
            { assertEquals("CGV 왕십리", savePort.saved[2].merchantName) },
        )
    }

    @Test
    fun returnsZeroCountsWhenNoCardsExist() {
        val service = SyncCardTransactionsService({ emptyList() }, { _, _ -> emptyList() }, { })

        val result = service.sync(SpendingPeriod(YearMonth.of(2026, 3)))

        assertAll(
            { assertEquals(0, result.fetchedCount) },
            { assertEquals(0, result.savedCount) },
            { assertEquals(0, result.syncedCardIds.size) },
        )
    }

    @Test
    fun rejectsNullPeriod() {
        val service = SyncCardTransactionsService({ emptyList() }, { _, _ -> emptyList() }, { })

        assertThrows(NullPointerException::class.java) { service.sync(null!!) }
    }

    private fun txn(cardId: String, amount: Long, day: Int, merchant: String, category: String) =
        SpendingRecord(UUID.randomUUID(), CardId(cardId), Money.won(amount), LocalDate.of(2026, 3, day), merchant, category)

    private class CapturingSavePort : SaveSpendingRecordPort {
        val saved = mutableListOf<SpendingRecord>()
        override fun save(spendingRecord: SpendingRecord) { saved.add(spendingRecord) }
    }
}
