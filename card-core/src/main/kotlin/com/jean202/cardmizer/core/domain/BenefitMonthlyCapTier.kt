package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money

data class BenefitMonthlyCapTier(
    val minimumPreviousMonthSpent: Money,
    val monthlyCap: Money,
)
