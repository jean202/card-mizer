package com.jean202.cardmizer.api.normalization

class NormalizedTransaction(
    val merchantCategory: String,
    paymentTags: Set<String>,
) {
    val paymentTags: Set<String>

    init {
        require(merchantCategory.isNotBlank()) { "merchantCategory must not be blank" }
        this.paymentTags = paymentTags.toSet()
    }
}
