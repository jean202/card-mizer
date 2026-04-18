package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money
import java.util.Locale

/**
 * 카드 혜택 규칙.
 *
 * Java 버전의 454줄 Builder 패턴을 Kotlin named parameters + defaults로 대체.
 * BenefitRule(ruleId = "...", benefitType = RATE_PERCENT, rateBasisPoints = 1000, ...) 형태로 직접 생성.
 */
class BenefitRule(
    val ruleId: String,
    val benefitSummary: String,
    val benefitType: BenefitType,
    merchantCategories: Set<String> = emptySet(),
    merchantKeywords: Set<String> = emptySet(),
    requiredTags: Set<String> = emptySet(),
    excludedTags: Set<String> = emptySet(),
    exclusiveGroupId: String? = null,
    sharedLimitGroupId: String? = null,
    val rateBasisPoints: Int = 0,
    val fixedBenefitAmount: Money = Money.ZERO,
    val minimumPaymentAmount: Money = Money.ZERO,
    val perTransactionCap: Money = Money.ZERO,
    val minimumPreviousMonthSpent: Money = Money.ZERO,
    monthlyCapTiers: List<BenefitMonthlyCapTier> = emptyList(),
    val yearlyBenefitCap: Money = Money.ZERO,
    val monthlyCountLimit: Int = 0,
    val yearlyCountLimit: Int = 0,
    sharedMonthlyCapTiers: List<BenefitMonthlyCapTier> = emptyList(),
    val sharedYearlyBenefitCap: Money = Money.ZERO,
) {
    val merchantCategories: Set<String>
    val merchantKeywords: Set<String>
    val requiredTags: Set<String>
    val excludedTags: Set<String>
    val exclusiveGroupId: String
    val sharedLimitGroupId: String?
    val monthlyCapTiers: List<BenefitMonthlyCapTier>
    val sharedMonthlyCapTiers: List<BenefitMonthlyCapTier>

    init {
        require(ruleId.isNotBlank()) { "ruleId must not be blank" }
        require(benefitSummary.isNotBlank()) { "benefitSummary must not be blank" }
        this.merchantCategories = normalizeCategories(merchantCategories)
        this.merchantKeywords = normalizeTokens(merchantKeywords)
        this.requiredTags = normalizeTokens(requiredTags)
        this.excludedTags = normalizeTokens(excludedTags)
        this.exclusiveGroupId = normalizeIdentifier(exclusiveGroupId ?: ruleId)
        this.sharedLimitGroupId = normalizeNullable(sharedLimitGroupId)
        this.monthlyCapTiers = monthlyCapTiers.toList()
        this.sharedMonthlyCapTiers = sharedMonthlyCapTiers.toList()
        require(monthlyCountLimit >= 0 && yearlyCountLimit >= 0) { "Benefit counts must not be negative" }
        validateBenefitValue()
    }

    fun matches(merchantName: String, merchantCategory: String, paymentTags: Set<String>): Boolean =
        matchesCategory(merchantCategory) && matchesMerchant(merchantName) && matchesTags(paymentTags)

    fun isEligible(
        paymentAmount: Money,
        merchantName: String,
        merchantCategory: String,
        paymentTags: Set<String>,
        previousMonthSpent: Money,
        usedMonthlyCount: Int,
        usedYearlyCount: Int,
    ): Boolean =
        matches(merchantName, merchantCategory, paymentTags)
            && paymentAmount.isGreaterThanOrEqual(minimumPaymentAmount)
            && previousMonthSpent.isGreaterThanOrEqual(minimumPreviousMonthSpent)
            && (monthlyCountLimit == 0 || usedMonthlyCount < monthlyCountLimit)
            && (yearlyCountLimit == 0 || usedYearlyCount < yearlyCountLimit)

    fun estimateRawBenefit(amount: Money): Money {
        val rawBenefit = when (benefitType) {
            BenefitType.RATE_PERCENT -> Money.won(amount.amount * rateBasisPoints / 10_000L)
            BenefitType.FIXED_AMOUNT -> fixedBenefitAmount
        }
        return if (perTransactionCap.amount == 0L) rawBenefit
        else Money.won(minOf(rawBenefit.amount, perTransactionCap.amount))
    }

    fun monthlyCapFor(previousMonthSpent: Money): Money = capFor(previousMonthSpent, monthlyCapTiers)

    fun sharedMonthlyCapFor(previousMonthSpent: Money): Money = capFor(previousMonthSpent, sharedMonthlyCapTiers)

    private fun matchesCategory(category: String): Boolean {
        val normalized = normalizeIdentifier(category)
        return merchantCategories.contains(ANY_CATEGORY) || merchantCategories.contains(normalized)
    }

    private fun matchesMerchant(merchantName: String): Boolean {
        if (merchantKeywords.isEmpty()) return true
        val normalized = merchantName.trim().uppercase(Locale.ROOT)
        return merchantKeywords.any { normalized.contains(it) }
    }

    private fun matchesTags(paymentTags: Set<String>): Boolean {
        val normalized = normalizeTokens(paymentTags)
        return normalized.containsAll(requiredTags) && excludedTags.none { normalized.contains(it) }
    }

    private fun capFor(previousMonthSpent: Money, capTiers: List<BenefitMonthlyCapTier>): Money {
        if (capTiers.isEmpty()) return Money.ZERO
        return capTiers
            .filter { previousMonthSpent.isGreaterThanOrEqual(it.minimumPreviousMonthSpent) }
            .maxByOrNull { it.minimumPreviousMonthSpent.amount }
            ?.monthlyCap ?: Money.ZERO
    }

    private fun validateBenefitValue() {
        when (benefitType) {
            BenefitType.RATE_PERCENT -> {
                require(rateBasisPoints in 1..10_000) { "rateBasisPoints must be between 1 and 10000" }
                require(fixedBenefitAmount.amount == 0L) { "fixedBenefitAmount must be zero for rate benefits" }
            }
            BenefitType.FIXED_AMOUNT -> {
                require(rateBasisPoints == 0) { "rateBasisPoints must be zero for fixed benefits" }
                require(fixedBenefitAmount.amount > 0L) { "fixedBenefitAmount must be positive for fixed benefits" }
            }
        }
    }

    companion object {
        private const val ANY_CATEGORY = "ANY"

        private fun normalizeCategories(categories: Set<String>): Set<String> {
            val normalized = normalizeTokens(categories)
            return if (normalized.isEmpty()) setOf(ANY_CATEGORY) else normalized
        }

        private fun normalizeTokens(values: Set<String>): Set<String> =
            values.map { normalizeIdentifier(it) }.toSet()

        private fun normalizeIdentifier(value: String): String {
            require(value.isNotBlank()) { "value must not be blank" }
            return value.trim().uppercase(Locale.ROOT)
        }

        private fun normalizeNullable(value: String?): String? =
            if (value.isNullOrBlank()) null else normalizeIdentifier(value)
    }
}
