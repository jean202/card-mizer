package com.jean202.cardmizer.api.demo

import java.time.LocalDate

class DemoSpendingRecordFixture(
    val cardId: String,
    val amount: Long,
    val spentOn: LocalDate,
    val merchantName: String,
    val merchantCategory: String?,
    paymentTags: Set<String>,
) {
    val paymentTags: Set<String>

    init {
        require(cardId.isNotBlank()) { "cardId must not be blank" }
        require(merchantName.isNotBlank()) { "merchantName must not be blank" }
        this.paymentTags = paymentTags.toSet()
    }
}
