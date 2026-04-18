package com.jean202.cardmizer.api.cards

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier
import com.jean202.cardmizer.core.domain.BenefitRule
import com.jean202.cardmizer.core.domain.BenefitType
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.port.`in`.GetCardPerformancePolicyUseCase
import com.jean202.cardmizer.core.port.`in`.ReplaceCardPerformancePolicyUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/api/cards/{cardId}/performance-policy")
class CardPerformancePolicyController(
    private val getCardPerformancePolicyUseCase: GetCardPerformancePolicyUseCase,
    private val replaceCardPerformancePolicyUseCase: ReplaceCardPerformancePolicyUseCase,
) {
    @GetMapping
    fun get(@PathVariable cardId: String): CardPerformancePolicyResponse =
        CardPerformancePolicyResponse.from(getCardPerformancePolicyUseCase.get(CardId(cardId)))

    @PutMapping
    fun replace(
        @PathVariable cardId: String,
        @Valid @RequestBody request: ReplaceCardPerformancePolicyRequest,
    ): CardPerformancePolicyResponse {
        val policy = request.toDomain(CardId(cardId))
        replaceCardPerformancePolicyUseCase.replace(policy)
        return CardPerformancePolicyResponse.from(policy)
    }

    @PatchMapping
    fun patch(
        @PathVariable cardId: String,
        @Valid @RequestBody request: PatchCardPerformancePolicyRequest,
    ): CardPerformancePolicyResponse {
        val domainCardId = CardId(cardId)
        val mergedPolicy = request.merge(domainCardId, getCardPerformancePolicyUseCase.get(domainCardId))
        replaceCardPerformancePolicyUseCase.replace(mergedPolicy)
        return CardPerformancePolicyResponse.from(mergedPolicy)
    }

    data class ReplaceCardPerformancePolicyRequest(
        @field:NotEmpty(message = "tiers must not be empty")
        @field:Valid
        val tiers: List<PerformanceTierRequest>,
        @field:Valid
        val benefitRules: List<BenefitRuleRequest>?,
    ) {
        fun toDomain(cardId: CardId) = CardPerformancePolicy(
            cardId = cardId,
            tiers = tiers.map { it.toDomain() },
            benefitRules = benefitRules?.map { it.toDomain() } ?: emptyList(),
        )
    }

    data class PatchCardPerformancePolicyRequest(
        @field:Valid
        val tiers: List<PerformanceTierRequest>?,
        @field:Valid
        val benefitRules: List<BenefitRuleRequest>?,
    ) {
        fun merge(cardId: CardId, currentPolicy: CardPerformancePolicy): CardPerformancePolicy {
            require(tiers != null || benefitRules != null) {
                "At least one of tiers or benefitRules must be provided"
            }
            return CardPerformancePolicy(
                cardId = cardId,
                tiers = tiers?.map { it.toDomain() } ?: currentPolicy.tiers,
                benefitRules = benefitRules?.map { it.toDomain() } ?: currentPolicy.benefitRules,
            )
        }
    }

    data class PerformanceTierRequest(
        @field:NotBlank(message = "code must not be blank")
        val code: String,
        @field:PositiveOrZero(message = "targetAmount must be zero or positive")
        val targetAmount: Long,
        @field:NotBlank(message = "benefitSummary must not be blank")
        val benefitSummary: String,
    ) {
        fun toDomain() = PerformanceTier(code, Money.won(targetAmount), benefitSummary)
    }

    data class BenefitRuleRequest(
        @field:NotBlank(message = "ruleId must not be blank")
        val ruleId: String,
        @field:NotBlank(message = "benefitSummary must not be blank")
        val benefitSummary: String,
        @field:NotBlank(message = "benefitType must not be blank")
        @field:Pattern(
            regexp = "(?i)^(RATE_PERCENT|FIXED_AMOUNT)$",
            message = "benefitType must be RATE_PERCENT or FIXED_AMOUNT",
        )
        val benefitType: String,
        val merchantCategories: Set<String>?,
        val merchantKeywords: Set<String>?,
        val requiredTags: Set<String>?,
        val excludedTags: Set<String>?,
        val exclusiveGroupId: String?,
        val sharedLimitGroupId: String?,
        @field:PositiveOrZero(message = "rateBasisPoints must be zero or positive")
        val rateBasisPoints: Int?,
        @field:PositiveOrZero(message = "fixedBenefitAmount must be zero or positive")
        val fixedBenefitAmount: Long?,
        @field:PositiveOrZero(message = "minimumPaymentAmount must be zero or positive")
        val minimumPaymentAmount: Long?,
        @field:PositiveOrZero(message = "perTransactionCap must be zero or positive")
        val perTransactionCap: Long?,
        @field:PositiveOrZero(message = "minimumPreviousMonthSpent must be zero or positive")
        val minimumPreviousMonthSpent: Long?,
        @field:Valid val monthlyCapTiers: List<BenefitMonthlyCapTierRequest>?,
        @field:PositiveOrZero(message = "yearlyBenefitCap must be zero or positive")
        val yearlyBenefitCap: Long?,
        @field:PositiveOrZero(message = "monthlyCountLimit must be zero or positive")
        val monthlyCountLimit: Int?,
        @field:PositiveOrZero(message = "yearlyCountLimit must be zero or positive")
        val yearlyCountLimit: Int?,
        @field:Valid val sharedMonthlyCapTiers: List<BenefitMonthlyCapTierRequest>?,
        @field:PositiveOrZero(message = "sharedYearlyBenefitCap must be zero or positive")
        val sharedYearlyBenefitCap: Long?,
    ) {
        fun toDomain(): BenefitRule {
            val parsedBenefitType = BenefitType.valueOf(benefitType.trim().uppercase(Locale.ROOT))
            val resolvedRateBasisPoints = when (parsedBenefitType) {
                BenefitType.RATE_PERCENT -> rateBasisPoints
                    ?: throw IllegalArgumentException("rateBasisPoints must not be null for RATE_PERCENT")
                BenefitType.FIXED_AMOUNT -> 0
            }
            val resolvedFixedAmount = when (parsedBenefitType) {
                BenefitType.FIXED_AMOUNT -> Money.won(fixedBenefitAmount
                    ?: throw IllegalArgumentException("fixedBenefitAmount must not be null for FIXED_AMOUNT"))
                BenefitType.RATE_PERCENT -> Money.ZERO
            }

            return BenefitRule(
                ruleId = ruleId,
                benefitSummary = benefitSummary,
                benefitType = parsedBenefitType,
                merchantCategories = merchantCategories ?: emptySet(),
                merchantKeywords = merchantKeywords ?: emptySet(),
                requiredTags = requiredTags ?: emptySet(),
                excludedTags = excludedTags ?: emptySet(),
                exclusiveGroupId = exclusiveGroupId,
                sharedLimitGroupId = sharedLimitGroupId,
                rateBasisPoints = resolvedRateBasisPoints,
                fixedBenefitAmount = resolvedFixedAmount,
                minimumPaymentAmount = Money.won(minimumPaymentAmount ?: 0L),
                perTransactionCap = Money.won(perTransactionCap ?: 0L),
                minimumPreviousMonthSpent = Money.won(minimumPreviousMonthSpent ?: 0L),
                monthlyCapTiers = monthlyCapTiers?.map { it.toDomain() } ?: emptyList(),
                yearlyBenefitCap = Money.won(yearlyBenefitCap ?: 0L),
                monthlyCountLimit = monthlyCountLimit ?: 0,
                yearlyCountLimit = yearlyCountLimit ?: 0,
                sharedMonthlyCapTiers = sharedMonthlyCapTiers?.map { it.toDomain() } ?: emptyList(),
                sharedYearlyBenefitCap = Money.won(sharedYearlyBenefitCap ?: 0L),
            )
        }
    }

    data class BenefitMonthlyCapTierRequest(
        @field:PositiveOrZero(message = "minimumPreviousMonthSpent must be zero or positive")
        val minimumPreviousMonthSpent: Long,
        @field:PositiveOrZero(message = "monthlyCap must be zero or positive")
        val monthlyCap: Long,
    ) {
        fun toDomain() = BenefitMonthlyCapTier(Money.won(minimumPreviousMonthSpent), Money.won(monthlyCap))
    }

    data class CardPerformancePolicyResponse(
        val cardId: String,
        val tiers: List<PerformanceTierResponse>,
        val benefitRules: List<BenefitRuleResponse>,
    ) {
        companion object {
            fun from(policy: CardPerformancePolicy) = CardPerformancePolicyResponse(
                cardId = policy.cardId.value,
                tiers = policy.tiers.map { PerformanceTierResponse.from(it) },
                benefitRules = policy.benefitRules.map { BenefitRuleResponse.from(it) },
            )
        }
    }

    data class PerformanceTierResponse(
        val code: String,
        val targetAmount: Long,
        val benefitSummary: String,
    ) {
        companion object {
            fun from(tier: PerformanceTier) = PerformanceTierResponse(
                code = tier.code,
                targetAmount = tier.targetAmount.amount,
                benefitSummary = tier.benefitSummary,
            )
        }
    }

    data class BenefitRuleResponse(
        val ruleId: String,
        val benefitSummary: String,
        val benefitType: String,
        val merchantCategories: List<String>,
        val merchantKeywords: List<String>,
        val requiredTags: List<String>,
        val excludedTags: List<String>,
        val exclusiveGroupId: String,
        val sharedLimitGroupId: String?,
        val rateBasisPoints: Int?,
        val fixedBenefitAmount: Long?,
        val minimumPaymentAmount: Long,
        val perTransactionCap: Long,
        val minimumPreviousMonthSpent: Long,
        val monthlyCapTiers: List<BenefitMonthlyCapTierResponse>,
        val yearlyBenefitCap: Long,
        val monthlyCountLimit: Int,
        val yearlyCountLimit: Int,
        val sharedMonthlyCapTiers: List<BenefitMonthlyCapTierResponse>,
        val sharedYearlyBenefitCap: Long,
    ) {
        companion object {
            fun from(rule: BenefitRule) = BenefitRuleResponse(
                ruleId = rule.ruleId,
                benefitSummary = rule.benefitSummary,
                benefitType = rule.benefitType.name,
                merchantCategories = rule.merchantCategories.sorted(),
                merchantKeywords = rule.merchantKeywords.sorted(),
                requiredTags = rule.requiredTags.sorted(),
                excludedTags = rule.excludedTags.sorted(),
                exclusiveGroupId = rule.exclusiveGroupId,
                sharedLimitGroupId = rule.sharedLimitGroupId,
                rateBasisPoints = if (rule.benefitType == BenefitType.RATE_PERCENT) rule.rateBasisPoints else null,
                fixedBenefitAmount = if (rule.benefitType == BenefitType.FIXED_AMOUNT) rule.fixedBenefitAmount.amount else null,
                minimumPaymentAmount = rule.minimumPaymentAmount.amount,
                perTransactionCap = rule.perTransactionCap.amount,
                minimumPreviousMonthSpent = rule.minimumPreviousMonthSpent.amount,
                monthlyCapTiers = rule.monthlyCapTiers.map { BenefitMonthlyCapTierResponse.from(it) },
                yearlyBenefitCap = rule.yearlyBenefitCap.amount,
                monthlyCountLimit = rule.monthlyCountLimit,
                yearlyCountLimit = rule.yearlyCountLimit,
                sharedMonthlyCapTiers = rule.sharedMonthlyCapTiers.map { BenefitMonthlyCapTierResponse.from(it) },
                sharedYearlyBenefitCap = rule.sharedYearlyBenefitCap.amount,
            )
        }
    }

    data class BenefitMonthlyCapTierResponse(
        val minimumPreviousMonthSpent: Long,
        val monthlyCap: Long,
    ) {
        companion object {
            fun from(tier: BenefitMonthlyCapTier) = BenefitMonthlyCapTierResponse(
                minimumPreviousMonthSpent = tier.minimumPreviousMonthSpent.amount,
                monthlyCap = tier.monthlyCap.amount,
            )
        }
    }
}
