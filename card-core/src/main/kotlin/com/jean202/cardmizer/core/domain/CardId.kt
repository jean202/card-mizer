package com.jean202.cardmizer.core.domain

@JvmInline
value class CardId(val value: String) {
    init {
        require(value.isNotBlank()) { "Card id must not be blank" }
    }
}
