package com.jean202.cardmizer.core.domain

data class Card(
    val id: CardId,
    val issuerName: String,
    val productName: String,
    val cardType: CardType,
    val priority: Int,
) {
    init {
        require(issuerName.isNotBlank()) { "Issuer name must not be blank" }
        require(productName.isNotBlank()) { "Product name must not be blank" }
        require(priority >= 1) { "Priority must start from 1" }
    }

    fun displayName(): String = "$issuerName $productName"
}
