package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class GetPerformanceOverviewServiceTest {
    @Test
    fun returnsMonthlyOverviewSortedByPriority() {
        val samsung = Card(CardId("SAMSUNG_MAIN"), "삼성카드", "대표 카드", CardType.CREDIT, 1)
        val hyundai = Card(CardId("HYUNDAI_MAIN"), "현대카드", "대표 카드", CardType.CREDIT, 2)

        val service = GetPerformanceOverviewService(
            { listOf(hyundai, samsung) },
            {
                listOf(
                    CardPerformancePolicy(
                        samsung.id,
                        listOf(
                            PerformanceTier("S1", Money.won(300_000), "기본 실적 구간"),
                            PerformanceTier("S2", Money.won(400_000), "상위 실적 구간"),
                        ),
                    ),
                    CardPerformancePolicy(
                        hyundai.id,
                        listOf(PerformanceTier("H1", Money.won(300_000), "기본 실적 구간")),
                    ),
                )
            },
            {
                listOf(
                    SpendingRecord(UUID.randomUUID(), samsung.id, Money.won(120_000), LocalDate.of(2026, 3, 4), "쿠팡", "ONLINE"),
                    SpendingRecord(UUID.randomUUID(), samsung.id, Money.won(80_000), LocalDate.of(2026, 3, 9), "배달", "FOOD"),
                    SpendingRecord(UUID.randomUUID(), hyundai.id, Money.won(300_000), LocalDate.of(2026, 3, 11), "주유", "AUTO"),
                )
            },
        )

        val overview = service.getOverview(SpendingPeriod(YearMonth.of(2026, 3)))

        assertEquals(2, overview.size)
        val first = overview[0]
        val second = overview[1]

        assertAll(
            { assertEquals("SAMSUNG_MAIN", first.card.id.value) },
            { assertEquals(200_000L, first.spentAmount.amount) },
            { assertEquals(300_000L, first.targetAmount.amount) },
            { assertEquals(100_000L, first.remainingAmount.amount) },
            { assertFalse(first.achieved) },
            { assertEquals("S1", first.targetTierCode) },
            { assertEquals("HYUNDAI_MAIN", second.card.id.value) },
            { assertEquals(300_000L, second.spentAmount.amount) },
            { assertEquals(300_000L, second.targetAmount.amount) },
            { assertEquals(0L, second.remainingAmount.amount) },
            { assertTrue(second.achieved) },
            { assertEquals("H1", second.targetTierCode) },
        )
    }
}
