package com.jean202.cardmizer.core.domain

data class RecommendationResult(
    val recommendedCard: Card,
    val reason: String,
    val alternatives: List<RecommendationCandidate>,
) {
    init {
        require(reason.isNotBlank()) { "Reason must not be blank" }
    }
}
