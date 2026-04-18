package com.jean202.cardmizer.api.cards

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.`in`.RegisterCardUseCase
import com.jean202.cardmizer.core.port.`in`.UpdatePriorityUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardManagementControllerTest {
    @Test
    fun convertsRegisterRequestToCardDomainModel() {
        val registerCardUseCase = CapturingRegisterCardUseCase()
        val controller = CardManagementController({ emptyList() }, registerCardUseCase, { })

        val response = controller.register(
            CardManagementController.RegisterCardRequest(
                cardId = "SHINHAN_MR_LIFE",
                issuerName = "신한카드",
                productName = "Mr.Life",
                cardType = "credit",
                priority = 2,
            ),
        )

        assertEquals("SHINHAN_MR_LIFE", registerCardUseCase.saved!!.id.value)
        assertEquals("신한카드 Mr.Life", response.cardName)
        assertEquals("CREDIT", response.cardType)
        assertEquals(2, response.priority)
    }

    @Test
    fun convertsPriorityRequestToOrderedCardIds() {
        val updatePriorityUseCase = CapturingUpdatePriorityUseCase()
        val controller = CardManagementController({ emptyList() }, { }, updatePriorityUseCase)

        controller.updatePriorities(
            CardManagementController.UpdatePrioritiesRequest(listOf("KB_NORI2_KBPAY", "SAMSUNG_KPASS")),
        )

        assertEquals(
            listOf("KB_NORI2_KBPAY", "SAMSUNG_KPASS"),
            updatePriorityUseCase.saved!!.orderedCardIds.map { it.value },
        )
    }

    private class CapturingRegisterCardUseCase : RegisterCardUseCase {
        var saved: Card? = null
        override fun register(card: Card) { saved = card }
    }

    private class CapturingUpdatePriorityUseCase : UpdatePriorityUseCase {
        var saved: PriorityStrategy? = null
        override fun update(priorityStrategy: PriorityStrategy) { saved = priorityStrategy }
    }
}
