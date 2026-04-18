package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort
import com.jean202.cardmizer.core.port.out.SaveCardPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RegisterCardServiceTest {
    @Test
    fun savesCardAndDefaultPolicy() {
        val existingCard = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)
        val saveCardPort = CapturingSaveCardPort()
        val savePolicyPort = CapturingSavePolicyPort()
        val service = RegisterCardService(
            { listOf(existingCard) },
            saveCardPort,
            savePolicyPort,
        )

        val newCard = Card(CardId("SHINHAN_MR_LIFE"), "신한카드", "Mr.Life", CardType.CREDIT, 2)
        service.register(newCard)

        assertEquals(newCard, saveCardPort.saved)
        assertEquals("SHINHAN_MR_LIFE", savePolicyPort.saved!!.cardId.value)
        assertEquals("DEFAULT_BASE", savePolicyPort.saved!!.highestTier().code)
    }

    @Test
    fun rejectsDuplicateCardId() {
        val existingCard = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)
        val service = RegisterCardService({ listOf(existingCard) }, { }, { })

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.register(Card(CardId("SAMSUNG_KPASS"), "삼성카드", "다른 카드", CardType.CREDIT, 1))
        }

        assertEquals("Card already exists: SAMSUNG_KPASS", exception.message)
    }

    @Test
    fun rejectsPriorityOutsideInsertRange() {
        val service = RegisterCardService(existingCards(), { }, { })

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.register(Card(CardId("SHINHAN_MR_LIFE"), "신한카드", "Mr.Life", CardType.CREDIT, 4))
        }

        assertEquals("Priority must be between 1 and 3", exception.message)
    }

    private fun existingCards(): LoadCardCatalogPort = LoadCardCatalogPort {
        listOf(
            Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
            Card(CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH", CardType.CREDIT, 2),
        )
    }

    private class CapturingSaveCardPort : SaveCardPort {
        var saved: Card? = null
        override fun save(card: Card) { saved = card }
    }

    private class CapturingSavePolicyPort : SaveCardPerformancePolicyPort {
        var saved: CardPerformancePolicy? = null
        override fun save(cardPerformancePolicy: CardPerformancePolicy) { saved = cardPerformancePolicy }
    }
}
