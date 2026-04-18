package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy

fun interface GetCardPerformancePolicyUseCase {
    fun get(cardId: CardId): CardPerformancePolicy
}
