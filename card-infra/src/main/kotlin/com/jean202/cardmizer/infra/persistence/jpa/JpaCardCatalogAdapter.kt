package com.jean202.cardmizer.infra.persistence.jpa

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.SaveCardPort
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Primary
@Transactional
class JpaCardCatalogAdapter(
    private val repository: JpaCardRepository,
) : LoadCardCatalogPort, SaveCardPort, UpdateCardPriorityPort {

    @Transactional(readOnly = true)
    override fun loadAll(): List<Card> =
        repository.findAllByOrderByPriorityAsc().map { toDomain(it) }

    override fun save(card: Card) {
        val existingCards = loadAll()
        val reorderedCards = mutableListOf<Card>()
        var inserted = false
        var nextPriority = 1

        for (existingCard in existingCards) {
            if (!inserted && nextPriority == card.priority) {
                reorderedCards.add(card.copy(priority = nextPriority))
                inserted = true
                nextPriority++
            }
            reorderedCards.add(existingCard.copy(priority = nextPriority))
            nextPriority++
        }

        if (!inserted) reorderedCards.add(card.copy(priority = nextPriority))

        repository.saveAll(reorderedCards.map { toEntity(it) })
    }

    override fun update(priorityStrategy: PriorityStrategy) {
        val cardsById = loadAll().associateBy { it.id }
        val reorderedCards = priorityStrategy.orderedCardIds.mapIndexed { index, cardId ->
            cardsById[cardId]?.copy(priority = index + 1)
                ?: throw IllegalArgumentException("Unknown card id: ${cardId.value}")
        }
        repository.saveAll(reorderedCards.map { toEntity(it) })
    }

    private fun toDomain(entity: JpaCardEntity) = Card(
        id = CardId(entity.id),
        issuerName = entity.issuerName,
        productName = entity.productName,
        cardType = CardType.valueOf(entity.cardType),
        priority = entity.priority,
    )

    private fun toEntity(card: Card) = JpaCardEntity(
        id = card.id.value,
        issuerName = card.issuerName,
        productName = card.productName,
        cardType = card.cardType.name,
        priority = card.priority,
    )
}
