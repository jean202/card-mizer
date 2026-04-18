package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.`in`.UpdatePriorityUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort

class UpdatePriorityService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val updateCardPriorityPort: UpdateCardPriorityPort,
) : UpdatePriorityUseCase {

    override fun update(priorityStrategy: PriorityStrategy) {
        val orderedCardIds = priorityStrategy.orderedCardIds
        val uniqueOrderedCardIds = orderedCardIds.toLinkedHashSet()
        require(uniqueOrderedCardIds.size == orderedCardIds.size) {
            "Priority strategy must not contain duplicate card ids"
        }

        val configuredCardIds = loadCardCatalogPort.loadAll().map { it.id }.toLinkedHashSet()
        if (configuredCardIds != uniqueOrderedCardIds) {
            val missingCardIds = configuredCardIds - uniqueOrderedCardIds
            val unknownCardIds = uniqueOrderedCardIds - configuredCardIds
            val detail = buildMismatchDetail(missingCardIds, unknownCardIds)
            throw IllegalArgumentException("Priority strategy must include every configured card exactly once$detail")
        }

        updateCardPriorityPort.update(priorityStrategy)
    }

    private fun buildMismatchDetail(missingCardIds: Set<CardId>, unknownCardIds: Set<CardId>): String {
        val parts = buildList {
            if (missingCardIds.isNotEmpty()) add("missing: ${formatCardIds(missingCardIds)}")
            if (unknownCardIds.isNotEmpty()) add("unknown: ${formatCardIds(unknownCardIds)}")
        }
        return if (parts.isEmpty()) "" else " (${parts.joinToString(" ")})"
    }

    private fun formatCardIds(cardIds: Set<CardId>) = cardIds.joinToString(", ") { it.value }

    private fun <T> List<T>.toLinkedHashSet(): LinkedHashSet<T> = LinkedHashSet(this)
}
