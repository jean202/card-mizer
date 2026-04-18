package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money

data class CardPerformancePolicy(
    val cardId: CardId,
    val tiers: List<PerformanceTier>,
    val benefitRules: List<BenefitRule> = emptyList(),
) {
    init {
        require(tiers.isNotEmpty()) { "At least one performance tier is required" }
    }

    fun nextTier(spentAmount: Money): PerformanceTier? =
        sortedTiers().firstOrNull { it.targetAmount > spentAmount }

    fun highestTier(): PerformanceTier = sortedTiers().last()

    fun isFullyAchieved(spentAmount: Money): Boolean = nextTier(spentAmount) == null

    fun estimateBenefit(
        amount: Money,
        merchantName: String,
        merchantCategory: String,
        paymentTags: Set<String>,
        previousMonthSpentAmount: Money,
        currentMonthRecords: List<SpendingRecord>,
        currentYearRecords: List<SpendingRecord>,
    ): BenefitQuote? {
        val state = simulateHistory(previousMonthSpentAmount, currentMonthRecords, currentYearRecords)
        val appliedBenefits = evaluateTransaction(
            amount, merchantName, merchantCategory,
            paymentTags, previousMonthSpentAmount, state,
        )
        if (appliedBenefits.isEmpty()) return null

        val appliedAmount = appliedBenefits.sumOf { it.benefitAmount.amount }
        val rawAmount = appliedBenefits.sumOf { it.rawBenefitAmount.amount }
        return BenefitQuote(appliedBenefits, Money.won(appliedAmount), Money.won(rawAmount))
    }

    private fun simulateHistory(
        previousMonthSpentAmount: Money,
        currentMonthRecords: List<SpendingRecord>,
        currentYearRecords: List<SpendingRecord>,
    ): EvaluationState {
        val state = EvaluationState()
        val currentMonthIds = currentMonthRecords.map { it.id }.toSet()
        val ordered = currentYearRecords.sortedWith(compareBy({ it.spentOn }, { it.id }))

        for (record in ordered) {
            val applied = evaluateTransaction(
                record.amount, record.merchantName, record.merchantCategory,
                record.paymentTags, previousMonthSpentAmount, state,
            )
            state.consume(applied, record.id in currentMonthIds)
        }
        return state
    }

    private fun evaluateTransaction(
        amount: Money,
        merchantName: String,
        merchantCategory: String,
        paymentTags: Set<String>,
        previousMonthSpentAmount: Money,
        state: EvaluationState,
    ): List<AppliedBenefit> {
        val selectedByGroup = selectBestByExclusiveGroup(
            amount, merchantName, merchantCategory, paymentTags, previousMonthSpentAmount, state,
        )
        if (selectedByGroup.isEmpty()) return emptyList()

        val appliedBenefits = mutableListOf<AppliedBenefit>()
        val sharedGroups = mutableMapOf<String, MutableList<RuleCandidate>>()

        for (candidate in selectedByGroup) {
            val ruleSpecific = applyRuleSpecificLimits(candidate, previousMonthSpentAmount, state)
            if (ruleSpecific.amount == 0L) continue

            val groupId = candidate.rule.sharedLimitGroupId
            if (groupId == null) {
                appliedBenefits += AppliedBenefit(candidate.rule, ruleSpecific, candidate.rawBenefitAmount)
            } else {
                sharedGroups.getOrPut(groupId) { mutableListOf() } += candidate.withRuleSpecificBenefit(ruleSpecific)
            }
        }

        for (groupCandidates in sharedGroups.values) {
            appliedBenefits += applySharedGroupLimits(groupCandidates, previousMonthSpentAmount, state)
        }
        return appliedBenefits
    }

    private fun selectBestByExclusiveGroup(
        amount: Money,
        merchantName: String,
        merchantCategory: String,
        paymentTags: Set<String>,
        previousMonthSpentAmount: Money,
        state: EvaluationState,
    ): List<RuleCandidate> {
        val bestByGroup = mutableMapOf<String, RuleCandidate>()
        for (rule in benefitRules) {
            if (!rule.isEligible(
                    amount, merchantName, merchantCategory, paymentTags,
                    previousMonthSpentAmount, state.monthlyRuleCount(rule), state.yearlyRuleCount(rule),
                )
            ) continue

            val raw = rule.estimateRawBenefit(amount)
            if (raw.amount == 0L) continue

            val candidate = RuleCandidate(rule, raw, Money.ZERO)
            bestByGroup.merge(rule.exclusiveGroupId, candidate) { left, right ->
                if (left.rawBenefitAmount.amount >= right.rawBenefitAmount.amount) left else right
            }
        }
        return bestByGroup.values.toList()
    }

    private fun applyRuleSpecificLimits(
        candidate: RuleCandidate,
        previousMonthSpentAmount: Money,
        state: EvaluationState,
    ): Money {
        var applied = candidate.rawBenefitAmount.amount
        val monthlyCap = candidate.rule.monthlyCapFor(previousMonthSpentAmount)
        if (monthlyCap.amount > 0) {
            val remaining = maxOf(0L, monthlyCap.amount - state.monthlyRuleBenefitUsed(candidate.rule).amount)
            applied = minOf(applied, remaining)
        }
        if (candidate.rule.yearlyBenefitCap.amount > 0) {
            val remaining = maxOf(0L, candidate.rule.yearlyBenefitCap.amount - state.yearlyRuleBenefitUsed(candidate.rule).amount)
            applied = minOf(applied, remaining)
        }
        return Money.won(maxOf(0L, applied))
    }

    private fun applySharedGroupLimits(
        candidates: List<RuleCandidate>,
        previousMonthSpentAmount: Money,
        state: EvaluationState,
    ): List<AppliedBenefit> {
        if (candidates.isEmpty()) return emptyList()
        val groupRule = candidates.first().rule
        var remaining = Long.MAX_VALUE

        val sharedMonthlyCap = groupRule.sharedMonthlyCapFor(previousMonthSpentAmount)
        if (sharedMonthlyCap.amount > 0) {
            remaining = minOf(remaining, maxOf(0L, sharedMonthlyCap.amount - state.monthlySharedBenefitUsed(groupRule.sharedLimitGroupId!!).amount))
        }
        if (groupRule.sharedYearlyBenefitCap.amount > 0) {
            remaining = minOf(remaining, maxOf(0L, groupRule.sharedYearlyBenefitCap.amount - state.yearlySharedBenefitUsed(groupRule.sharedLimitGroupId!!).amount))
        }

        val applied = mutableListOf<AppliedBenefit>()
        for (candidate in candidates.sortedByDescending { it.ruleSpecificBenefit.amount }) {
            if (remaining == 0L) break
            var amount = candidate.ruleSpecificBenefit.amount
            if (remaining != Long.MAX_VALUE) amount = minOf(amount, remaining)
            if (amount == 0L) continue
            applied += AppliedBenefit(candidate.rule, Money.won(amount), candidate.rawBenefitAmount)
            if (remaining != Long.MAX_VALUE) remaining -= amount
        }
        return applied
    }

    private fun sortedTiers(): List<PerformanceTier> =
        tiers.sortedBy { it.targetAmount.amount }

    private data class RuleCandidate(
        val rule: BenefitRule,
        val rawBenefitAmount: Money,
        val ruleSpecificBenefit: Money,
    ) {
        fun withRuleSpecificBenefit(benefit: Money) = copy(ruleSpecificBenefit = benefit)
    }

    private class EvaluationState {
        private val monthlyRuleBenefit = mutableMapOf<String, Money>()
        private val yearlyRuleBenefit = mutableMapOf<String, Money>()
        private val monthlyRuleCount = mutableMapOf<String, Int>()
        private val yearlyRuleCount = mutableMapOf<String, Int>()
        private val monthlySharedBenefit = mutableMapOf<String, Money>()
        private val yearlySharedBenefit = mutableMapOf<String, Money>()

        fun monthlyRuleCount(rule: BenefitRule) = monthlyRuleCount.getOrDefault(rule.ruleId, 0)
        fun yearlyRuleCount(rule: BenefitRule) = yearlyRuleCount.getOrDefault(rule.ruleId, 0)
        fun monthlyRuleBenefitUsed(rule: BenefitRule) = monthlyRuleBenefit.getOrDefault(rule.ruleId, Money.ZERO)
        fun yearlyRuleBenefitUsed(rule: BenefitRule) = yearlyRuleBenefit.getOrDefault(rule.ruleId, Money.ZERO)
        fun monthlySharedBenefitUsed(groupId: String) = monthlySharedBenefit.getOrDefault(groupId, Money.ZERO)
        fun yearlySharedBenefitUsed(groupId: String) = yearlySharedBenefit.getOrDefault(groupId, Money.ZERO)

        fun consume(appliedBenefits: List<AppliedBenefit>, isCurrentMonth: Boolean) {
            val yearlyCountedRules = mutableSetOf<String>()
            val monthlyCountedRules = mutableSetOf<String>()

            for (benefit in appliedBenefits) {
                val ruleId = benefit.benefitRule.ruleId
                yearlyRuleBenefit.merge(ruleId, benefit.benefitAmount, Money::add)
                yearlyCountedRules += ruleId

                benefit.benefitRule.sharedLimitGroupId?.let { groupId ->
                    yearlySharedBenefit.merge(groupId, benefit.benefitAmount, Money::add)
                }

                if (isCurrentMonth) {
                    monthlyRuleBenefit.merge(ruleId, benefit.benefitAmount, Money::add)
                    monthlyCountedRules += ruleId
                    benefit.benefitRule.sharedLimitGroupId?.let { groupId ->
                        monthlySharedBenefit.merge(groupId, benefit.benefitAmount, Money::add)
                    }
                }
            }
            yearlyCountedRules.forEach { yearlyRuleCount.merge(it, 1, Int::plus) }
            if (isCurrentMonth) monthlyCountedRules.forEach { monthlyRuleCount.merge(it, 1, Int::plus) }
        }
    }
}
