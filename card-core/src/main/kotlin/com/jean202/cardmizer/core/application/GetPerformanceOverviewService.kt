package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.GetPerformanceOverviewUseCase
import com.jean202.cardmizer.core.port.`in`.GetPerformanceOverviewUseCase.CardPerformanceSnapshot
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort

class GetPerformanceOverviewService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
    private val loadSpendingRecordsPort: LoadSpendingRecordsPort,
) : GetPerformanceOverviewUseCase {

    override fun getOverview(period: SpendingPeriod): List<CardPerformanceSnapshot> {
        val policiesByCardId = loadCardPerformancePoliciesPort.loadAll().associateBy { it.cardId }
        val spentByCardId = aggregateSpentAmounts(loadSpendingRecordsPort.loadByPeriod(period))

        return loadCardCatalogPort.loadAll()
            .sortedBy { it.priority }
            .map { card ->
                toSnapshot(
                    card,
                    policiesByCardId[card.id],
                    spentByCardId.getOrDefault(card.id, Money.ZERO),
                )
            }
    }

    private fun aggregateSpentAmounts(spendingRecords: List<SpendingRecord>): Map<CardId, Money> =
        spendingRecords.groupBy { it.cardId }
            .mapValues { (_, records) -> records.fold(Money.ZERO) { acc, r -> acc + r.amount } }

    private fun toSnapshot(card: Card, policy: CardPerformancePolicy?, spentAmount: Money): CardPerformanceSnapshot {
        policy ?: throw IllegalStateException("No performance policy configured for card: ${card.id.value}")

        val achieved = policy.isFullyAchieved(spentAmount)
        val targetTier = policy.nextTier(spentAmount) ?: policy.highestTier()
        val targetAmount = targetTier.targetAmount
        val remainingAmount = if (achieved) Money.ZERO else targetAmount - spentAmount

        return CardPerformanceSnapshot(
            card = card,
            spentAmount = spentAmount,
            targetAmount = targetAmount,
            remainingAmount = remainingAmount,
            achieved = achieved,
            targetTierCode = targetTier.code,
        )
    }
}
