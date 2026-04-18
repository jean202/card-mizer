package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PerformanceTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GetCardPerformancePolicyServiceTest {
    @Test
    fun returnsPolicyForConfiguredCard() {
        val cardId = CardId("SAMSUNG_KPASS")
        val service = GetCardPerformancePolicyService(
            { listOf(Card(cardId, "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)) },
            { listOf(CardPerformancePolicy(cardId, listOf(PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")))) },
        )

        val policy = service.get(cardId)

        assertEquals("SAMSUNG_KPASS", policy.cardId.value)
        assertEquals("KPASS_40", policy.highestTier().code)
    }

    @Test
    fun rejectsUnknownCard() {
        val service = GetCardPerformancePolicyService({ emptyList() }, { emptyList() })

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            service.get(CardId("UNKNOWN_CARD"))
        }

        assertEquals("Card not found: UNKNOWN_CARD", exception.message)
    }
}
