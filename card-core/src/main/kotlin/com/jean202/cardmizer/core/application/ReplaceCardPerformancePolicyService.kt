package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.port.`in`.ReplaceCardPerformancePolicyUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort

class ReplaceCardPerformancePolicyService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val replaceCardPerformancePolicyPort: ReplaceCardPerformancePolicyPort,
) : ReplaceCardPerformancePolicyUseCase {

    override fun replace(cardPerformancePolicy: CardPerformancePolicy) {
        if (loadCardCatalogPort.loadAll().none { it.id == cardPerformancePolicy.cardId }) {
            throw ResourceNotFoundException("Card not found: ${cardPerformancePolicy.cardId.value}")
        }

        replaceCardPerformancePolicyPort.replace(cardPerformancePolicy)
    }
}
