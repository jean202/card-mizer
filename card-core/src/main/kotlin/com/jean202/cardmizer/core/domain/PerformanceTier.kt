package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money

data class PerformanceTier(
    val code: String,
    val targetAmount: Money,
    val benefitSummary: String,
) {
    init {
        require(code.isNotBlank()) { "Tier code must not be blank" }
        require(benefitSummary.isNotBlank()) { "Benefit summary must not be blank" }
    }
}
