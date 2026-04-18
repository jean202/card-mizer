package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money

data class AppliedBenefit(
    val benefitRule: BenefitRule,
    val benefitAmount: Money,
    val rawBenefitAmount: Money,
) {
    fun wasCapped(): Boolean = rawBenefitAmount.amount > benefitAmount.amount
}
