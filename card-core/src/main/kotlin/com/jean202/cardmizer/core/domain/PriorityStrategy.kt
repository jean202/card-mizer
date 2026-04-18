package com.jean202.cardmizer.core.domain

data class PriorityStrategy(val orderedCardIds: List<CardId>) {
    init {
        require(orderedCardIds.isNotEmpty()) { "Priority strategy must contain at least one card" }
    }
}
