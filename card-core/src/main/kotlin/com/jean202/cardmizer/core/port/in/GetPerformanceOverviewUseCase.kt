package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.SpendingPeriod

interface GetPerformanceOverviewUseCase {
    fun getOverview(period: SpendingPeriod): List<CardPerformanceSnapshot>

    data class CardPerformanceSnapshot(
        val card: Card,
        val spentAmount: Money,
        val targetAmount: Money,
        val remainingAmount: Money,
        val achieved: Boolean,
        val targetTierCode: String,
    ) {
        init {
            require(targetTierCode.isNotBlank()) { "targetTierCode must not be blank" }
        }
    }
}
