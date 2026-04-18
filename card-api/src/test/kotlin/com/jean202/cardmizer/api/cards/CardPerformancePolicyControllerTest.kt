package com.jean202.cardmizer.api.cards

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier
import com.jean202.cardmizer.core.domain.BenefitRule
import com.jean202.cardmizer.core.domain.BenefitType
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.port.`in`.ReplaceCardPerformancePolicyUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardPerformancePolicyControllerTest {
    @Test
    fun returnsPolicyResponse() {
        val policy = CardPerformancePolicy(
            CardId("SAMSUNG_KPASS"),
            listOf(PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
            listOf(
                BenefitRule(
                    ruleId = "KPASS_TRANSIT",
                    benefitSummary = "대중교통 10% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    rateBasisPoints = 1000,
                    merchantCategories = setOf("PUBLIC_TRANSIT"),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                ),
            ),
        )
        val controller = CardPerformancePolicyController({ policy }, { })

        val response = controller.get("SAMSUNG_KPASS")

        assertEquals("SAMSUNG_KPASS", response.cardId)
        assertEquals("KPASS_40", response.tiers[0].code)
        assertEquals("KPASS_TRANSIT", response.benefitRules[0].ruleId)
        assertEquals(1000, response.benefitRules[0].rateBasisPoints)
    }

    @Test
    fun convertsReplaceRequestToDomainPolicy() {
        val replaceUseCase = CapturingReplaceUseCase()
        val controller = CardPerformancePolicyController({ replaceUseCase.saved!! }, replaceUseCase)

        val response = controller.replace(
            "SHINHAN_MR_LIFE",
            CardPerformancePolicyController.ReplaceCardPerformancePolicyRequest(
                tiers = listOf(
                    CardPerformancePolicyController.PerformanceTierRequest("LIFE_30", 300_000, "전월 30만원 이상 혜택 구간"),
                ),
                benefitRules = listOf(
                    CardPerformancePolicyController.BenefitRuleRequest(
                        ruleId = "LIFE_OTT",
                        benefitSummary = "OTT 10% 할인",
                        benefitType = "RATE_PERCENT",
                        merchantCategories = setOf("OTT"),
                        merchantKeywords = emptySet(),
                        requiredTags = setOf("SUBSCRIPTION"),
                        excludedTags = emptySet(),
                        exclusiveGroupId = "LIFE_PRIMARY",
                        sharedLimitGroupId = null,
                        rateBasisPoints = 1000,
                        fixedBenefitAmount = null,
                        minimumPaymentAmount = 0L,
                        perTransactionCap = 0L,
                        minimumPreviousMonthSpent = 300_000L,
                        monthlyCapTiers = listOf(CardPerformancePolicyController.BenefitMonthlyCapTierRequest(0L, 10_000L)),
                        yearlyBenefitCap = 0L,
                        monthlyCountLimit = 0,
                        yearlyCountLimit = 0,
                        sharedMonthlyCapTiers = emptyList(),
                        sharedYearlyBenefitCap = 0L,
                    ),
                ),
            ),
        )

        assertEquals("SHINHAN_MR_LIFE", replaceUseCase.saved!!.cardId.value)
        assertEquals("LIFE_30", replaceUseCase.saved!!.highestTier().code)
        assertEquals("LIFE_OTT", replaceUseCase.saved!!.benefitRules[0].ruleId)
        assertEquals(1000, replaceUseCase.saved!!.benefitRules[0].rateBasisPoints)
        assertEquals("SHINHAN_MR_LIFE", response.cardId)
    }

    @Test
    fun mergesPatchRequestWithCurrentPolicy() {
        val currentPolicy = CardPerformancePolicy(
            CardId("SHINHAN_MR_LIFE"),
            listOf(PerformanceTier("LIFE_30", Money.won(300_000), "전월 30만원 이상 혜택 구간")),
            listOf(
                BenefitRule(
                    ruleId = "LIFE_OTT",
                    benefitSummary = "OTT 10% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    rateBasisPoints = 1000,
                    merchantCategories = setOf("OTT"),
                ),
            ),
        )
        val replaceUseCase = CapturingReplaceUseCase()
        val controller = CardPerformancePolicyController({ currentPolicy }, replaceUseCase)

        val response = controller.patch(
            "SHINHAN_MR_LIFE",
            CardPerformancePolicyController.PatchCardPerformancePolicyRequest(
                tiers = null,
                benefitRules = listOf(
                    CardPerformancePolicyController.BenefitRuleRequest(
                        ruleId = "LIFE_MART",
                        benefitSummary = "마트 5천원 할인",
                        benefitType = "FIXED_AMOUNT",
                        merchantCategories = setOf("MART"),
                        merchantKeywords = emptySet(),
                        requiredTags = emptySet(),
                        excludedTags = emptySet(),
                        exclusiveGroupId = null,
                        sharedLimitGroupId = null,
                        rateBasisPoints = null,
                        fixedBenefitAmount = 5_000L,
                        minimumPaymentAmount = 0L,
                        perTransactionCap = 0L,
                        minimumPreviousMonthSpent = 0L,
                        monthlyCapTiers = emptyList(),
                        yearlyBenefitCap = 0L,
                        monthlyCountLimit = 0,
                        yearlyCountLimit = 0,
                        sharedMonthlyCapTiers = emptyList(),
                        sharedYearlyBenefitCap = 0L,
                    ),
                ),
            ),
        )

        assertEquals("LIFE_30", replaceUseCase.saved!!.highestTier().code)
        assertEquals("LIFE_MART", replaceUseCase.saved!!.benefitRules[0].ruleId)
        assertEquals(5_000L, replaceUseCase.saved!!.benefitRules[0].fixedBenefitAmount.amount)
        assertEquals("SHINHAN_MR_LIFE", response.cardId)
        assertEquals("LIFE_30", response.tiers[0].code)
        assertEquals("LIFE_MART", response.benefitRules[0].ruleId)
    }

    private class CapturingReplaceUseCase : ReplaceCardPerformancePolicyUseCase {
        var saved: CardPerformancePolicy? = null
        override fun replace(cardPerformancePolicy: CardPerformancePolicy) { saved = cardPerformancePolicy }
    }
}
