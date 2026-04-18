package com.jean202.cardmizer.infra.persistence

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
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryCardPerformancePolicyAdapter :
    LoadCardPerformancePoliciesPort, SaveCardPerformancePolicyPort, ReplaceCardPerformancePolicyPort {

    private val policies = CopyOnWriteArrayList(initialPolicies())

    override fun loadAll(): List<CardPerformancePolicy> = policies.toList()

    override fun save(cardPerformancePolicy: CardPerformancePolicy) {
        require(policies.none { it.cardId == cardPerformancePolicy.cardId }) {
            "Card policy already exists: ${cardPerformancePolicy.cardId.value}"
        }
        policies.add(cardPerformancePolicy)
    }

    @Synchronized
    override fun replace(cardPerformancePolicy: CardPerformancePolicy) {
        val index = policies.indexOfFirst { it.cardId == cardPerformancePolicy.cardId }
        if (index >= 0) policies[index] = cardPerformancePolicy else policies.add(cardPerformancePolicy)
    }

    companion object {
        private fun initialPolicies() = listOf(
            kPassSamsungPolicy(),
            myWeshPolicy(),
            nori2KbPayPolicy(),
            hyundaiZeroPointPolicy(),
        )

        private fun kPassSamsungPolicy() = CardPerformancePolicy(
            cardId = CardId("SAMSUNG_KPASS"),
            tiers = listOf(
                PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간"),
                PerformanceTier("KPASS_80", Money.won(800_000), "전월 80만원 이상 혜택 구간"),
            ),
            benefitRules = listOf(
                BenefitRule(
                    ruleId = "KPASS_TRANSIT",
                    benefitSummary = "대중교통 10% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("PUBLIC_TRANSIT"),
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(5_000)),
                        BenefitMonthlyCapTier(Money.won(800_000), Money.won(10_000)),
                    ),
                    rateBasisPoints = 1000,
                ),
                BenefitRule(
                    ruleId = "KPASS_COFFEE",
                    benefitSummary = "커피전문점 20% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("COFFEE"),
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                        BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000)),
                    ),
                    rateBasisPoints = 2000,
                ),
                BenefitRule(
                    ruleId = "KPASS_DIGITAL",
                    benefitSummary = "디지털콘텐츠/멤버십 20% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("DIGITAL_CONTENT", "MEMBERSHIP", "OTT"),
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                        BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000)),
                    ),
                    rateBasisPoints = 2000,
                ),
                BenefitRule(
                    ruleId = "KPASS_ONLINE",
                    benefitSummary = "온라인쇼핑 3% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("ONLINE_SHOPPING", "ONLINE_FASHION"),
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                        BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000)),
                    ),
                    rateBasisPoints = 300,
                ),
            ),
        )

        private fun myWeshPolicy() = CardPerformancePolicy(
            cardId = CardId("KB_MY_WESH"),
            tiers = listOf(PerformanceTier("WESH_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
            benefitRules = listOf(
                BenefitRule(
                    ruleId = "WESH_KB_PAY",
                    benefitSummary = "KB Pay 10% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("ANY"),
                    requiredTags = setOf("KB_PAY"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    perTransactionCap = Money.won(2_500),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                    rateBasisPoints = 1000,
                ),
                BenefitRule(
                    ruleId = "WESH_FOOD",
                    benefitSummary = "음식점/편의점 10% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("RESTAURANT", "CONVENIENCE_STORE"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    perTransactionCap = Money.won(2_500),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                    rateBasisPoints = 1000,
                ),
                BenefitRule(
                    ruleId = "WESH_MOBILE",
                    benefitSummary = "이동통신요금 10% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("MOBILE_BILL"),
                    requiredTags = setOf("AUTO_BILL"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                    rateBasisPoints = 1000,
                ),
                BenefitRule(
                    ruleId = "WESH_OTT",
                    benefitSummary = "OTT 30% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("OTT"),
                    requiredTags = setOf("SUBSCRIPTION"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                    rateBasisPoints = 3000,
                ),
                BenefitRule(
                    ruleId = "WESH_PLAY",
                    benefitSummary = "노는데 진심 택시/커피 5% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("TAXI", "COFFEE"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                    rateBasisPoints = 500,
                ),
                BenefitRule(
                    ruleId = "WESH_MOVIE",
                    benefitSummary = "노는데 진심 영화관 30% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("MOVIE"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    perTransactionCap = Money.won(5_000),
                    yearlyBenefitCap = Money.won(20_000),
                    yearlyCountLimit = 4,
                    rateBasisPoints = 3000,
                ),
            ),
        )

        private fun nori2KbPayPolicy(): CardPerformancePolicy {
            val noriSharedCapTiers = listOf(
                BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000)),
                BenefitMonthlyCapTier(Money.won(600_000), Money.won(40_000)),
                BenefitMonthlyCapTier(Money.won(800_000), Money.won(50_000)),
            )

            return CardPerformancePolicy(
                cardId = CardId("KB_NORI2_KBPAY"),
                tiers = listOf(
                    PerformanceTier("NORI_20", Money.won(200_000), "전월 20만원 이상 혜택 구간"),
                    PerformanceTier("NORI_40", Money.won(400_000), "전월 40만원 이상 혜택 구간"),
                    PerformanceTier("NORI_60", Money.won(600_000), "전월 60만원 이상 혜택 구간"),
                    PerformanceTier("NORI_80", Money.won(800_000), "전월 80만원 이상 혜택 구간"),
                ),
                benefitRules = listOf(
                    BenefitRule(
                        ruleId = "NORI_COFFEE",
                        benefitSummary = "커피 10% 할인",
                        benefitType = BenefitType.RATE_PERCENT,
                        merchantCategories = setOf("COFFEE"),
                        exclusiveGroupId = "NORI_DAILY",
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(3_000))),
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                        rateBasisPoints = 1000,
                    ),
                    BenefitRule(
                        ruleId = "NORI_CULTURE",
                        benefitSummary = "문화 10% 할인",
                        benefitType = BenefitType.RATE_PERCENT,
                        merchantCategories = setOf("CULTURE"),
                        merchantKeywords = setOf("INTERPARK", "인터파크"),
                        exclusiveGroupId = "NORI_DAILY",
                        minimumPreviousMonthSpent = Money.won(200_000),
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(7_000))),
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                        rateBasisPoints = 1000,
                    ),
                    BenefitRule(
                        ruleId = "NORI_DELIVERY",
                        benefitSummary = "배달 1,000원 할인",
                        benefitType = BenefitType.FIXED_AMOUNT,
                        merchantCategories = setOf("FOOD_DELIVERY"),
                        merchantKeywords = setOf("배달의민족", "요기요"),
                        exclusiveGroupId = "NORI_DAILY",
                        minimumPreviousMonthSpent = Money.won(200_000),
                        fixedBenefitAmount = Money.won(1_000),
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(1_000))),
                        monthlyCountLimit = 1,
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                    ),
                    BenefitRule(
                        ruleId = "NORI_MOVIE",
                        benefitSummary = "영화 4,000원 할인",
                        benefitType = BenefitType.FIXED_AMOUNT,
                        merchantCategories = setOf("MOVIE"),
                        merchantKeywords = setOf("CGV"),
                        exclusiveGroupId = "NORI_DAILY",
                        minimumPreviousMonthSpent = Money.won(200_000),
                        fixedBenefitAmount = Money.won(4_000),
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(8_000))),
                        monthlyCountLimit = 2,
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                    ),
                    BenefitRule(
                        ruleId = "NORI_KBPAY_OFFLINE",
                        benefitSummary = "KB Pay 오프라인 2% 추가 할인",
                        benefitType = BenefitType.RATE_PERCENT,
                        merchantCategories = setOf("ANY"),
                        requiredTags = setOf("KB_PAY", "OFFLINE"),
                        exclusiveGroupId = "NORI_KBPAY_BONUS",
                        minimumPreviousMonthSpent = Money.won(300_000),
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(3_000))),
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                        rateBasisPoints = 200,
                    ),
                    BenefitRule(
                        ruleId = "NORI_KBPAY_ONLINE",
                        benefitSummary = "KB Pay 온라인 2% 추가 할인",
                        benefitType = BenefitType.RATE_PERCENT,
                        merchantCategories = setOf("ANY"),
                        requiredTags = setOf("KB_PAY", "ONLINE"),
                        exclusiveGroupId = "NORI_KBPAY_BONUS",
                        minimumPreviousMonthSpent = Money.won(300_000),
                        monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(2_000))),
                        sharedLimitGroupId = "NORI_TOTAL",
                        sharedMonthlyCapTiers = noriSharedCapTiers,
                        rateBasisPoints = 200,
                    ),
                ),
            )
        }

        private fun hyundaiZeroPointPolicy() = CardPerformancePolicy(
            cardId = CardId("HYUNDAI_ZERO_POINT"),
            tiers = listOf(PerformanceTier("ZERO_FREE", Money.ZERO, "실적 조건 없는 기본 적립")),
            benefitRules = listOf(
                BenefitRule(
                    ruleId = "ZERO_BASE",
                    benefitSummary = "국내외 가맹점 1% M포인트 적립",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("ANY"),
                    exclusiveGroupId = "ZERO_BASELINE",
                    rateBasisPoints = 100,
                ),
                BenefitRule(
                    ruleId = "ZERO_LIFESTYLE",
                    benefitSummary = "생활 필수 영역 2.5% M포인트 적립",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("BIG_MART", "CONVENIENCE_STORE", "RESTAURANT", "COFFEE", "PUBLIC_TRANSIT"),
                    exclusiveGroupId = "ZERO_BASELINE",
                    rateBasisPoints = 250,
                ),
                BenefitRule(
                    ruleId = "ZERO_SIMPLE_PAY",
                    benefitSummary = "온라인 간편결제 2.5% M포인트 적립",
                    benefitType = BenefitType.RATE_PERCENT,
                    merchantCategories = setOf("ANY"),
                    requiredTags = setOf("SIMPLE_PAY_ONLINE"),
                    exclusiveGroupId = "ZERO_BASELINE",
                    rateBasisPoints = 250,
                ),
            ),
        )
    }
}
