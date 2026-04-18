package com.jean202.cardmizer.infra.persistence

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.SaveCardPort
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryCardCatalogAdapter : LoadCardCatalogPort, SaveCardPort, UpdateCardPriorityPort {
    private val cards = CopyOnWriteArrayList(initialCards())

    override fun loadAll(): List<Card> = cards.sortedBy { it.priority }

    @Synchronized
    override fun save(card: Card) {
        require(cards.none { it.id == card.id }) { "Card already exists: ${card.id.value}" }

        val reorderedCards = mutableListOf<Card>()
        var inserted = false
        var nextPriority = 1

        for (existingCard in loadAll()) {
            if (!inserted && nextPriority == card.priority) {
                reorderedCards.add(card.copy(priority = nextPriority))
                inserted = true
                nextPriority++
            }
            reorderedCards.add(existingCard.copy(priority = nextPriority))
            nextPriority++
        }

        if (!inserted) reorderedCards.add(card.copy(priority = nextPriority))

        cards.clear()
        cards.addAll(reorderedCards)
    }

    @Synchronized
    override fun update(priorityStrategy: PriorityStrategy) {
        val cardsById = loadAll().associateBy { it.id }
        val reorderedCards = priorityStrategy.orderedCardIds.mapIndexed { index, cardId ->
            cardsById[cardId]?.copy(priority = index + 1)
                ?: throw IllegalArgumentException("Unknown card id: ${cardId.value}")
        }
        cards.clear()
        cards.addAll(reorderedCards)
    }

    companion object {
        private fun initialCards() = listOf(
            Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
            Card(CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH KB국민카드", CardType.CREDIT, 2),
            Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 3),
            Card(CardId("HYUNDAI_ZERO_POINT"), "현대카드", "ZERO Edition2(포인트형)", CardType.CREDIT, 4),
        )
    }
}
