package com.jean202.cardmizer.api.demo

class DemoRecommendationRequest(
    val spendingMonth: String,
    val amount: Long,
    val merchantName: String,
    val merchantCategory: String?,
    paymentTags: Set<String>,
) {
    val paymentTags: Set<String>

    init {
        require(spendingMonth.isNotBlank()) { "spendingMonth must not be blank" }
        require(merchantName.isNotBlank()) { "merchantName must not be blank" }
        this.paymentTags = paymentTags.toSet()
    }
}
