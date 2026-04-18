package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.CardPerformancePolicy

fun interface ReplaceCardPerformancePolicyUseCase {
    fun replace(cardPerformancePolicy: CardPerformancePolicy)
}
