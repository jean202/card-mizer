package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UpdatePriorityServiceTest {
    @Test
    fun updatesPriorityWhenStrategyMatchesConfiguredCards() {
        val updatePort = CapturingUpdatePort()
        val service = UpdatePriorityService(::configuredCards, updatePort)
        val strategy = PriorityStrategy(listOf(CardId("KB_NORI2_KBPAY"), CardId("SAMSUNG_KPASS")))

        service.update(strategy)

        assertEquals(strategy, updatePort.saved)
    }

    @Test
    fun rejectsDuplicateCardIds() {
        val service = UpdatePriorityService(::configuredCards, { })

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.update(PriorityStrategy(listOf(CardId("SAMSUNG_KPASS"), CardId("SAMSUNG_KPASS"))))
        }

        assertEquals("Priority strategy must not contain duplicate card ids", exception.message)
    }

    @Test
    fun rejectsMissingOrUnknownCards() {
        val service = UpdatePriorityService(::configuredCards, { })

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.update(PriorityStrategy(listOf(CardId("SAMSUNG_KPASS"), CardId("UNKNOWN"))))
        }

        assertEquals(
            "Priority strategy must include every configured card exactly once (missing: KB_NORI2_KBPAY unknown: UNKNOWN)",
            exception.message,
        )
    }

    private fun configuredCards() = listOf(
        Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
        Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 2),
    )

    private class CapturingUpdatePort : UpdateCardPriorityPort {
        var saved: PriorityStrategy? = null
        override fun update(priorityStrategy: PriorityStrategy) { saved = priorityStrategy }
    }
}
