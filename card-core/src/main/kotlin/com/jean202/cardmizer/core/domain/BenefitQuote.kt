package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money

data class BenefitQuote(
    val appliedBenefits: List<AppliedBenefit>,
    val benefitAmount: Money,
    val rawBenefitAmount: Money,
) {
    init {
        require(appliedBenefits.isNotEmpty()) { "appliedBenefits must not be empty" }
    }

    fun wasCapped(): Boolean = rawBenefitAmount.amount > benefitAmount.amount

    fun summary(): String =
        appliedBenefits
            .map { it.benefitRule.benefitSummary }
            .distinct()
            .joinToString(", ")
}
