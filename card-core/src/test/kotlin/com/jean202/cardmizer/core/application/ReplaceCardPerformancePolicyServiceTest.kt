package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReplaceCardPerformancePolicyServiceTest {
    @Test
    fun replacesPolicyForConfiguredCard() {
        val replacePort = CapturingReplacePort()
        val cardId = CardId("SAMSUNG_KPASS")
        val service = ReplaceCardPerformancePolicyService(
            { listOf(Card(cardId, "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)) },
            replacePort,
        )
        val policy = CardPerformancePolicy(
            cardId,
            listOf(PerformanceTier("KPASS_50", Money.won(500_000), "전월 50만원 이상 혜택 구간")),
        )

        service.replace(policy)

        assertEquals(policy, replacePort.saved)
    }

    @Test
    fun rejectsUnknownCard() {
        val service = ReplaceCardPerformancePolicyService({ emptyList() }, { })

        val policy = CardPerformancePolicy(
            CardId("UNKNOWN_CARD"),
            listOf(PerformanceTier("DEFAULT", Money.ZERO, "기본 정책")),
        )

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            service.replace(policy)
        }

        assertEquals("Card not found: UNKNOWN_CARD", exception.message)
    }

    private class CapturingReplacePort : ReplaceCardPerformancePolicyPort {
        var saved: CardPerformancePolicy? = null

        override fun replace(cardPerformancePolicy: CardPerformancePolicy) {
            saved = cardPerformancePolicy
        }
    }
}
