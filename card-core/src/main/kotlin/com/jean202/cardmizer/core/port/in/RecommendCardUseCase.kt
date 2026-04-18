package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.RecommendationResult

fun interface RecommendCardUseCase {
    fun recommend(context: RecommendationContext): RecommendationResult
}
