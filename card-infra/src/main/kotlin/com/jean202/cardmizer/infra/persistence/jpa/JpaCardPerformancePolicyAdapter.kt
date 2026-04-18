package com.jean202.cardmizer.infra.persistence.jpa

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier
import com.jean202.cardmizer.core.domain.BenefitRule
import com.jean202.cardmizer.core.domain.BenefitType
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.UncheckedIOException

@Component
@Primary
@Transactional
class JpaCardPerformancePolicyAdapter(
    private val repository: JpaCardPerformancePolicyRepository,
    private val objectMapper: ObjectMapper,
) : LoadCardPerformancePoliciesPort, SaveCardPerformancePolicyPort, ReplaceCardPerformancePolicyPort {

    @Transactional(readOnly = true)
    override fun loadAll(): List<CardPerformancePolicy> =
        repository.findAll().map { toDomain(it) }

    override fun save(cardPerformancePolicy: CardPerformancePolicy) {
        if (repository.existsById(cardPerformancePolicy.cardId.value)) {
            throw IllegalArgumentException("Card policy already exists: ${cardPerformancePolicy.cardId.value}")
        }
        repository.save(toEntity(cardPerformancePolicy))
    }

    override fun replace(cardPerformancePolicy: CardPerformancePolicy) {
        repository.save(toEntity(cardPerformancePolicy))
    }

    private fun toEntity(policy: CardPerformancePolicy) = JpaCardPerformancePolicyEntity(
        cardId = policy.cardId.value,
        tiersJson = writeJson(policy.tiers.map { PerformanceTierDocument.from(it) }),
        benefitRulesJson = writeJson(policy.benefitRules.map { BenefitRuleDocument.from(it) }),
    )

    private fun toDomain(entity: JpaCardPerformancePolicyEntity): CardPerformancePolicy {
        val tiers = readJson<List<PerformanceTierDocument>>(
            entity.tiersJson,
            object : TypeReference<List<PerformanceTierDocument>>() {},
        ).map { it.toDomain() }
        val benefitRules = readJson<List<BenefitRuleDocument>>(
            entity.benefitRulesJson,
            object : TypeReference<List<BenefitRuleDocument>>() {},
        ).map { it.toDomain() }
        return CardPerformancePolicy(CardId(entity.cardId), tiers, benefitRules)
    }

    private fun writeJson(value: Any): String =
        try { objectMapper.writeValueAsString(value) }
        catch (e: Exception) { throw UncheckedIOException("Failed to serialize card performance policy", java.io.IOException(e)) }

    private fun <T> readJson(value: String, typeReference: TypeReference<T>): T =
        try { objectMapper.readValue(value, typeReference) }
        catch (e: Exception) { throw UncheckedIOException("Failed to deserialize card performance policy", java.io.IOException(e)) }

    private data class PerformanceTierDocument(
        val code: String,
        val targetAmount: Long,
        val benefitSummary: String,
    ) {
        fun toDomain() = PerformanceTier(code, Money.won(targetAmount), benefitSummary)

        companion object {
            fun from(tier: PerformanceTier) = PerformanceTierDocument(
                code = tier.code,
                targetAmount = tier.targetAmount.amount,
                benefitSummary = tier.benefitSummary,
            )
        }
    }

    private data class BenefitMonthlyCapTierDocument(
        val minimumPreviousMonthSpent: Long,
        val monthlyCap: Long,
    ) {
        fun toDomain() = BenefitMonthlyCapTier(
            Money.won(minimumPreviousMonthSpent),
            Money.won(monthlyCap),
        )

        companion object {
            fun from(tier: BenefitMonthlyCapTier) = BenefitMonthlyCapTierDocument(
                minimumPreviousMonthSpent = tier.minimumPreviousMonthSpent.amount,
                monthlyCap = tier.monthlyCap.amount,
            )
        }
    }

    private data class BenefitRuleDocument(
        val ruleId: String,
        val benefitSummary: String,
        val benefitType: BenefitType,
        val merchantCategories: Set<String>?,
        val merchantKeywords: Set<String>?,
        val requiredTags: Set<String>?,
        val excludedTags: Set<String>?,
        val exclusiveGroupId: String?,
        val sharedLimitGroupId: String?,
        val rateBasisPoints: Int?,
        val fixedBenefitAmount: Long?,
        val minimumPaymentAmount: Long?,
        val perTransactionCap: Long?,
        val minimumPreviousMonthSpent: Long?,
        val monthlyCapTiers: List<BenefitMonthlyCapTierDocument>?,
        val yearlyBenefitCap: Long?,
        val monthlyCountLimit: Int?,
        val yearlyCountLimit: Int?,
        val sharedMonthlyCapTiers: List<BenefitMonthlyCapTierDocument>?,
        val sharedYearlyBenefitCap: Long?,
    ) {
        fun toDomain() = BenefitRule(
            ruleId = ruleId,
            benefitSummary = benefitSummary,
            benefitType = benefitType,
            merchantCategories = merchantCategories ?: emptySet(),
            merchantKeywords = merchantKeywords ?: emptySet(),
            requiredTags = requiredTags ?: emptySet(),
            excludedTags = excludedTags ?: emptySet(),
            exclusiveGroupId = exclusiveGroupId,
            sharedLimitGroupId = sharedLimitGroupId,
            rateBasisPoints = rateBasisPoints ?: 0,
            fixedBenefitAmount = Money.won(fixedBenefitAmount ?: 0L),
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

        companion object {
            fun from(rule: BenefitRule) = BenefitRuleDocument(
                ruleId = rule.ruleId,
                benefitSummary = rule.benefitSummary,
                benefitType = rule.benefitType,
                merchantCategories = rule.merchantCategories,
                merchantKeywords = rule.merchantKeywords,
                requiredTags = rule.requiredTags,
                excludedTags = rule.excludedTags,
                exclusiveGroupId = rule.exclusiveGroupId,
                sharedLimitGroupId = rule.sharedLimitGroupId,
                rateBasisPoints = if (rule.benefitType == BenefitType.RATE_PERCENT) rule.rateBasisPoints else null,
                fixedBenefitAmount = if (rule.benefitType == BenefitType.FIXED_AMOUNT) rule.fixedBenefitAmount.amount else null,
                minimumPaymentAmount = rule.minimumPaymentAmount.amount,
                perTransactionCap = rule.perTransactionCap.amount,
                minimumPreviousMonthSpent = rule.minimumPreviousMonthSpent.amount,
                monthlyCapTiers = rule.monthlyCapTiers.map { BenefitMonthlyCapTierDocument.from(it) },
                yearlyBenefitCap = rule.yearlyBenefitCap.amount,
                monthlyCountLimit = rule.monthlyCountLimit,
                yearlyCountLimit = rule.yearlyCountLimit,
                sharedMonthlyCapTiers = rule.sharedMonthlyCapTiers.map { BenefitMonthlyCapTierDocument.from(it) },
                sharedYearlyBenefitCap = rule.sharedYearlyBenefitCap.amount,
            )
        }
    }
}
