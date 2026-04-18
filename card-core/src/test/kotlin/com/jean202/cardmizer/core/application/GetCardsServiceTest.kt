package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetCardsServiceTest {
    @Test
    fun returnsAllCardsSortedByPriority() {
        val highPriority = Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2", CardType.CHECK, 3)
        val lowPriority = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스", CardType.CREDIT, 1)
        val midPriority = Card(CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH", CardType.CREDIT, 2)

        val service = GetCardsService { listOf(highPriority, lowPriority, midPriority) }

        val result = service.getAll()

        assertEquals(3, result.size)
        assertEquals("SAMSUNG_KPASS", result[0].id.value)
        assertEquals("KB_MY_WESH", result[1].id.value)
        assertEquals("KB_NORI2_KBPAY", result[2].id.value)
    }

    @Test
    fun returnsEmptyListWhenNoCards() {
        val service = GetCardsService { emptyList() }

        val result = service.getAll()

        assertEquals(0, result.size)
    }
}
