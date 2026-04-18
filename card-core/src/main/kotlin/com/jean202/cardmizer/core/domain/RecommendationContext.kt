package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money
import java.util.Locale

class RecommendationContext(
    val spendingPeriod: SpendingPeriod,
    val amount: Money,
    merchantName: String,
    merchantCategory: String? = null,
    paymentTags: Set<String> = emptySet(),
) {
    val merchantName: String
    val merchantCategory: String
    val paymentTags: Set<String>

    init {
        require(merchantName.isNotBlank()) { "Merchant name must not be blank" }
        this.merchantName = merchantName.trim()
        this.merchantCategory =
            if (merchantCategory.isNullOrBlank()) "UNCATEGORIZED"
            else normalizeValue(merchantCategory)
        this.paymentTags = normalizeValues(paymentTags)
    }

    companion object {
        private fun normalizeValue(value: String): String =
            value.trim().uppercase(Locale.ROOT)

        private fun normalizeValues(rawTags: Set<String>): Set<String> =
            rawTags.filter { it.isNotBlank() }.map { normalizeValue(it) }.toSet()
    }
}
