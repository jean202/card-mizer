package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.CardPerformancePolicy

fun interface ReplaceCardPerformancePolicyPort {
    fun replace(cardPerformancePolicy: CardPerformancePolicy)
}
