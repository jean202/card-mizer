package com.jean202.cardmizer.core.domain

import com.jean202.cardmizer.common.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class CardPerformancePolicyBenefitTest {
    @Test
    fun stacksNoriMovieDiscountWithKbPayBonusWithinSharedCap() {
        val policy = CardPerformancePolicy(
            CardId("KB_NORI2_KBPAY"),
            listOf(PerformanceTier("NORI_20", Money.won(200_000), "전월 20만원 이상 혜택 구간")),
            listOf(
                BenefitRule(
                    ruleId = "NORI_MOVIE",
                    benefitSummary = "영화 4,000원 할인",
                    benefitType = BenefitType.FIXED_AMOUNT,
                    fixedBenefitAmount = Money.won(4_000),
                    merchantCategories = setOf("MOVIE"),
                    merchantKeywords = setOf("CGV"),
                    exclusiveGroupId = "NORI_DAILY",
                    minimumPreviousMonthSpent = Money.won(200_000),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(8_000))),
                    monthlyCountLimit = 2,
                    sharedLimitGroupId = "NORI_TOTAL",
                    sharedMonthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000)),
                    ),
                ),
                BenefitRule(
                    ruleId = "NORI_KBPAY_OFFLINE",
                    benefitSummary = "KB Pay 오프라인 2% 추가 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    rateBasisPoints = 200,
                    requiredTags = setOf("KB_PAY", "OFFLINE"),
                    exclusiveGroupId = "NORI_KBPAY_BONUS",
                    minimumPreviousMonthSpent = Money.won(300_000),
                    monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(3_000))),
                    sharedLimitGroupId = "NORI_TOTAL",
                    sharedMonthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000)),
                    ),
                ),
            ),
        )

        val quote = policy.estimateBenefit(
            Money.won(15_000),
            "CGV 강남",
            "MOVIE",
            setOf("KB_PAY", "OFFLINE"),
            Money.won(300_000),
            emptyList(),
            emptyList(),
        )

        assertTrue(quote != null)
        assertEquals(4_300L, quote!!.benefitAmount.amount)
        assertTrue(quote.summary().contains("영화 4,000원 할인"))
        assertTrue(quote.summary().contains("KB Pay 오프라인 2% 추가 할인"))
    }

    @Test
    fun capsKPassCoffeeBenefitByPreviousMonthTier() {
        val policy = CardPerformancePolicy(
            CardId("SAMSUNG_KPASS"),
            listOf(PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
            listOf(
                BenefitRule(
                    ruleId = "KPASS_COFFEE",
                    benefitSummary = "커피전문점 20% 결제일할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    rateBasisPoints = 2000,
                    merchantCategories = setOf("COFFEE"),
                    minimumPreviousMonthSpent = Money.won(400_000),
                    monthlyCapTiers = listOf(
                        BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                        BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000)),
                    ),
                ),
            ),
        )

        val quote = policy.estimateBenefit(
            Money.won(50_000),
            "스타벅스 역삼",
            "COFFEE",
            emptySet(),
            Money.won(450_000),
            emptyList(),
            emptyList(),
        )

        assertTrue(quote != null)
        assertEquals(4_000L, quote!!.benefitAmount.amount)
        assertTrue(quote.wasCapped())
    }

    @Test
    fun stopsMyWeshMovieBenefitAfterYearlyLimitIsConsumed() {
        val policy = CardPerformancePolicy(
            CardId("KB_MY_WESH"),
            listOf(PerformanceTier("WESH_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
            listOf(
                BenefitRule(
                    ruleId = "WESH_MOVIE",
                    benefitSummary = "노는데 진심 영화관 30% 할인",
                    benefitType = BenefitType.RATE_PERCENT,
                    rateBasisPoints = 3000,
                    merchantCategories = setOf("MOVIE"),
                    exclusiveGroupId = "WESH_PRIMARY",
                    minimumPreviousMonthSpent = Money.won(400_000),
                    perTransactionCap = Money.won(5_000),
                    yearlyBenefitCap = Money.won(20_000),
                    yearlyCountLimit = 4,
                ),
            ),
        )

        val currentYearRecords = listOf(
            spendingRecord(LocalDate.of(2026, 1, 4), "CGV 홍대", "MOVIE"),
            spendingRecord(LocalDate.of(2026, 2, 7), "CGV 왕십리", "MOVIE"),
            spendingRecord(LocalDate.of(2026, 2, 21), "CGV 왕십리", "MOVIE"),
            spendingRecord(LocalDate.of(2026, 3, 8), "CGV 여의도", "MOVIE"),
        )

        val quote = policy.estimateBenefit(
            Money.won(18_000),
            "CGV 용산",
            "MOVIE",
            emptySet(),
            Money.won(450_000),
            listOf(currentYearRecords[3]),
            currentYearRecords,
        )

        assertTrue(quote == null)
    }

    private fun spendingRecord(spentOn: LocalDate, merchantName: String, merchantCategory: String) =
        SpendingRecord(UUID.randomUUID(), CardId("KB_MY_WESH"), Money.won(18_000), spentOn, merchantName, merchantCategory)
}
