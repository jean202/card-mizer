package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier
import com.jean202.cardmizer.core.domain.BenefitRule
import com.jean202.cardmizer.core.domain.BenefitType
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardPerformancePolicy
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PerformanceTier
import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class RecommendCardServiceTest {
    @Test
    fun recommendsSamsungKPassWhenTransitBenefitAlsoCompletesThreshold() {
        val samsung = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)
        val hyundai = Card(CardId("HYUNDAI_ZERO_POINT"), "현대카드", "ZERO Edition2(포인트형)", CardType.CREDIT, 4)

        val service = RecommendCardService(
            { listOf(samsung, hyundai) },
            {
                listOf(
                    CardPerformancePolicy(
                        samsung.id,
                        listOf(PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                        listOf(
                            BenefitRule(
                                ruleId = "KPASS_TRANSIT",
                                benefitSummary = "대중교통 10% 결제일할인",
                                benefitType = BenefitType.RATE_PERCENT,
                                rateBasisPoints = 1000,
                                merchantCategories = setOf("PUBLIC_TRANSIT"),
                                minimumPreviousMonthSpent = Money.won(400_000),
                                monthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000))),
                            ),
                        ),
                    ),
                    CardPerformancePolicy(
                        hyundai.id,
                        listOf(PerformanceTier("ZERO_FREE", Money.ZERO, "실적 조건 없는 기본 적립")),
                        listOf(
                            BenefitRule(
                                ruleId = "ZERO_BASE",
                                benefitSummary = "국내외 가맹점 1% M포인트 적립",
                                benefitType = BenefitType.RATE_PERCENT,
                                rateBasisPoints = 100,
                                exclusiveGroupId = "ZERO_BASELINE",
                            ),
                        ),
                    ),
                )
            },
            { period ->
                when (period.yearMonth) {
                    YearMonth.of(2026, 2) -> listOf(
                        spendingRecord(samsung.id, 450_000, LocalDate.of(2026, 2, 10), "생활비", "GENERAL_MERCHANT"),
                        spendingRecord(hyundai.id, 150_000, LocalDate.of(2026, 2, 8), "생활비", "GENERAL_MERCHANT"),
                    )
                    YearMonth.of(2026, 3) -> listOf(
                        spendingRecord(samsung.id, 380_000, LocalDate.of(2026, 3, 5), "교통비", "PUBLIC_TRANSIT"),
                        spendingRecord(hyundai.id, 90_000, LocalDate.of(2026, 3, 7), "마트", "BIG_MART"),
                    )
                    else -> emptyList()
                }
            },
        )

        val result = service.recommend(
            RecommendationContext(SpendingPeriod(YearMonth.of(2026, 3)), Money.won(20_000), "서울교통공사", "PUBLIC_TRANSIT"),
        )

        assertAll(
            { assertEquals("SAMSUNG_KPASS", result.recommendedCard.id.value) },
            { assertTrue(result.reason.contains("바로 달성")) },
            { assertTrue(result.reason.contains("2,000원")) },
            { assertEquals("HYUNDAI_ZERO_POINT", result.alternatives[0].card.id.value) },
        )
    }

    @Test
    fun prefersNoriWhenKbPayBonusStacksWithMovieDiscount() {
        val nori = Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 3)
        val samsung = Card(CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)

        val service = RecommendCardService(
            { listOf(nori, samsung) },
            {
                listOf(
                    CardPerformancePolicy(
                        nori.id,
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
                                sharedMonthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000))),
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
                                sharedMonthlyCapTiers = listOf(BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000))),
                            ),
                        ),
                    ),
                    CardPerformancePolicy(
                        samsung.id,
                        listOf(PerformanceTier("KPASS_20", Money.won(200_000), "기준 구간")),
                    ),
                )
            },
            { period ->
                when (period.yearMonth) {
                    YearMonth.of(2026, 2) -> listOf(
                        spendingRecord(nori.id, 300_000, LocalDate.of(2026, 2, 10), "생활비", "GENERAL_MERCHANT"),
                        spendingRecord(samsung.id, 100_000, LocalDate.of(2026, 2, 9), "생활비", "GENERAL_MERCHANT"),
                    )
                    YearMonth.of(2026, 3) -> listOf(
                        spendingRecord(nori.id, 150_000, LocalDate.of(2026, 3, 5), "마트", "BIG_MART"),
                        spendingRecord(samsung.id, 155_000, LocalDate.of(2026, 3, 7), "쇼핑", "ONLINE_SHOPPING"),
                    )
                    else -> emptyList()
                }
            },
        )

        val result = service.recommend(
            RecommendationContext(
                SpendingPeriod(YearMonth.of(2026, 3)),
                Money.won(15_000),
                "CGV 왕십리",
                "MOVIE",
                setOf("KB_PAY", "OFFLINE"),
            ),
        )

        assertAll(
            { assertEquals("KB_NORI2_KBPAY", result.recommendedCard.id.value) },
            { assertTrue(result.reason.contains("4,300원")) },
            { assertEquals("SAMSUNG_KPASS", result.alternatives[0].card.id.value) },
        )
    }

    private fun spendingRecord(cardId: CardId, amount: Long, spentOn: LocalDate, merchantName: String, merchantCategory: String) =
        SpendingRecord(UUID.randomUUID(), cardId, Money.won(amount), spentOn, merchantName, merchantCategory)
}
