package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.port.`in`.RegisterCardUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort
import com.jean202.cardmizer.core.port.out.SaveCardPort

class RegisterCardService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val saveCardPort: SaveCardPort,
    private val saveCardPerformancePolicyPort: SaveCardPerformancePolicyPort,
) : RegisterCardUseCase {

    override fun register(card: Card) {
        val existingCards = loadCardCatalogPort.loadAll()

        require(existingCards.none { it.id == card.id }) { "Card already exists: ${card.id.value}" }

        val maxAllowedPriority = existingCards.size + 1
        require(card.priority <= maxAllowedPriority) { "Priority must be between 1 and $maxAllowedPriority" }

        saveCardPort.save(card)
        saveCardPerformancePolicyPort.save(defaultPolicyFor(card))
    }

    private fun defaultPolicyFor(card: Card) = CardPerformancePolicy(
        card.id,
        listOf(PerformanceTier("DEFAULT_BASE", Money.ZERO, "실적 조건 미설정")),
    )
}
