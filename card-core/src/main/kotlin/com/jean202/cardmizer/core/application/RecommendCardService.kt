package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.BenefitQuote
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.domain.RecommendationCandidate
import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.RecommendationResult
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.RecommendCardUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort

class RecommendCardService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
    private val loadSpendingRecordsPort: LoadSpendingRecordsPort,
) : RecommendCardUseCase {

    override fun recommend(context: RecommendationContext): RecommendationResult {
        val spendingRecords = loadSpendingRecordsPort.loadByPeriod(context.spendingPeriod)
        val previousMonthRecords = loadSpendingRecordsPort.loadByPeriod(context.spendingPeriod.previous())
        val yearToDateRecords = loadYearToDateRecords(context)
        val policiesByCardId = loadCardPerformancePoliciesPort.loadAll().associateBy { it.cardId }
        val spentByCardId = aggregateSpentAmounts(spendingRecords)
        val previousMonthSpentByCardId = aggregateSpentAmounts(previousMonthRecords)
        val recordsByCardId = spendingRecords.groupBy { it.cardId }
        val yearToDateRecordsByCardId = yearToDateRecords.groupBy { it.cardId }

        val scoredRecommendations = loadCardCatalogPort.loadAll()
            .map { card ->
                score(
                    card,
                    policiesByCardId[card.id],
                    spentByCardId.getOrDefault(card.id, Money.ZERO),
                    previousMonthSpentByCardId.getOrDefault(card.id, Money.ZERO),
                    recordsByCardId.getOrDefault(card.id, emptyList()),
                    yearToDateRecordsByCardId.getOrDefault(card.id, emptyList()),
                    context,
                )
            }
            .sortedWith(compareByDescending<ScoredRecommendation> { it.score }.thenBy { it.card.priority })

        check(scoredRecommendations.isNotEmpty()) { "No cards configured for recommendation" }

        val topRecommendation = scoredRecommendations.first()
        val alternatives = scoredRecommendations.drop(1)
            .map { RecommendationCandidate(it.card, it.reason, it.score) }

        return RecommendationResult(topRecommendation.card, topRecommendation.reason, alternatives)
    }

    private fun aggregateSpentAmounts(spendingRecords: List<SpendingRecord>): Map<CardId, Money> =
        spendingRecords.groupBy { it.cardId }
            .mapValues { (_, records) -> records.fold(Money.ZERO) { acc, r -> acc + r.amount } }

    private fun score(
        card: Card,
        policy: CardPerformancePolicy?,
        currentSpentAmount: Money,
        previousMonthSpentAmount: Money,
        currentMonthRecords: List<SpendingRecord>,
        currentYearRecords: List<SpendingRecord>,
        context: RecommendationContext,
    ): ScoredRecommendation {
        policy ?: throw IllegalStateException("No performance policy configured for card: ${card.id.value}")

        val projectedSpentAmount = currentSpentAmount + context.amount
        val nextTierBefore = policy.nextTier(currentSpentAmount)
        val benefitQuote = policy.estimateBenefit(
            context.amount,
            context.merchantName,
            context.merchantCategory,
            context.paymentTags,
            previousMonthSpentAmount,
            currentMonthRecords,
            currentYearRecords,
        )
        val reachesNextTier = nextTierBefore?.let { projectedSpentAmount >= it.targetAmount } ?: false
        val alreadyFullyAchieved = policy.isFullyAchieved(currentSpentAmount)

        val score = computeScore(
            card, policy, currentSpentAmount, projectedSpentAmount,
            reachesNextTier, alreadyFullyAchieved, benefitQuote,
        )
        val reason = buildReason(
            card, currentSpentAmount, projectedSpentAmount, nextTierBefore,
            reachesNextTier, alreadyFullyAchieved, benefitQuote,
        )

        return ScoredRecommendation(card, score, reason)
    }

    private fun computeScore(
        card: Card,
        policy: CardPerformancePolicy,
        currentSpentAmount: Money,
        projectedSpentAmount: Money,
        reachesNextTier: Boolean,
        alreadyFullyAchieved: Boolean,
        benefitQuote: BenefitQuote?,
    ): Int {
        val priorityBonus = maxOf(0, 100 - card.priority)
        val benefitBonus = minOf(
            benefitQuote?.let { it.benefitAmount.amount / 10L } ?: 0L,
            1_500L,
        ).toInt()

        if (reachesNextTier) {
            val nextTier = policy.nextTier(currentSpentAmount)!!
            val overshoot = projectedSpentAmount.amount - nextTier.targetAmount.amount
            val overshootPenalty = minOf(overshoot / 1_000L, 200L).toInt()
            return 10_000 + priorityBonus - overshootPenalty + benefitBonus
        }

        if (alreadyFullyAchieved) {
            return 1_000 + priorityBonus + benefitBonus
        }

        val nextTier = policy.nextTier(currentSpentAmount)!!
        val remainingAfterPayment = maxOf(0L, nextTier.targetAmount.amount - projectedSpentAmount.amount)
        val remainingPenalty = minOf(remainingAfterPayment / 1_000L, 5_000L).toInt()
        return 5_000 + priorityBonus - remainingPenalty + benefitBonus
    }

    private fun buildReason(
        card: Card,
        currentSpentAmount: Money,
        projectedSpentAmount: Money,
        nextTierBefore: PerformanceTier?,
        reachesNextTier: Boolean,
        alreadyFullyAchieved: Boolean,
        benefitQuote: BenefitQuote?,
    ): String {
        val benefitSuffix = benefitQuote?.let { formatBenefitSuffix(it) } ?: ""

        if (reachesNextTier) {
            val nextTier = nextTierBefore!!
            val remainingBeforePayment = nextTier.targetAmount.amount - currentSpentAmount.amount
            return "${card.displayName()} ${nextTier.code} 구간까지 %,d원 남아 있어 이번 결제로 바로 달성할 수 있습니다."
                .format(remainingBeforePayment) + benefitSuffix
        }

        if (alreadyFullyAchieved) {
            return "${card.displayName()}는 이미 최고 실적 구간을 달성한 상태라, 이번 결제는 우선순위 기준으로만 추천합니다.$benefitSuffix"
        }

        val nextTier = nextTierBefore!!
        val remainingAfterPayment = maxOf(0L, nextTier.targetAmount.amount - projectedSpentAmount.amount)
        return "${card.displayName()}는 이번 결제 후 ${nextTier.code} 구간까지 %,d원이 남아 다음 실적 구간에 가장 가깝습니다."
            .format(remainingAfterPayment) + benefitSuffix
    }

    private fun formatBenefitSuffix(quote: BenefitQuote): String {
        val capSuffix = if (quote.wasCapped()) " 월 한도로 일부만 반영되었습니다." else ""
        val formattedAmount = "%,d".format(quote.benefitAmount.amount)
        return " 예상 혜택은 ${formattedAmount}원(${quote.summary()})입니다.$capSuffix"
    }

    private fun loadYearToDateRecords(context: RecommendationContext): List<SpendingRecord> {
        val seen = LinkedHashMap<Any, SpendingRecord>()
        context.spendingPeriod.fromStartOfYear()
            .flatMap { loadSpendingRecordsPort.loadByPeriod(it) }
            .forEach { seen.putIfAbsent(it.id, it) }
        return seen.values.toList()
    }

    private data class ScoredRecommendation(val card: Card, val score: Int, val reason: String)
}
