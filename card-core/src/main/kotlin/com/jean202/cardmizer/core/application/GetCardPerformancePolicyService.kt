package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.port.`in`.GetCardPerformancePolicyUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort

class GetCardPerformancePolicyService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
) : GetCardPerformancePolicyUseCase {

    override fun get(cardId: CardId): CardPerformancePolicy {
        if (loadCardCatalogPort.loadAll().none { it.id == cardId }) {
            throw ResourceNotFoundException("Card not found: ${cardId.value}")
        }

        return loadCardPerformancePoliciesPort.loadAll()
            .firstOrNull { it.cardId == cardId }
            ?: throw ResourceNotFoundException("Card performance policy not found: ${cardId.value}")
    }
}
