package com.jean202.cardmizer.core.domain

data class RecommendationCandidate(
    val card: Card,
    val reason: String,
    val score: Int,
) {
    init {
        require(reason.isNotBlank()) { "Reason must not be blank" }
        require(score >= 0) { "Score must not be negative" }
    }
}
