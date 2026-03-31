package com.jean202.cardmizer.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.common.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardPerformancePolicyBenefitTest {
    @Test
    void stacksNoriMovieDiscountWithKbPayBonusWithinSharedCap() {
        CardPerformancePolicy policy = new CardPerformancePolicy(
                new CardId("KB_NORI2_KBPAY"),
                List.of(new PerformanceTier("NORI_20", Money.won(200_000), "전월 20만원 이상 혜택 구간")),
                List.of(
                        BenefitRule.fixedAmount("NORI_MOVIE", "영화 4,000원 할인", Money.won(4_000))
                                .categories("MOVIE")
                                .merchantKeywords("CGV")
                                .exclusiveGroup("NORI_DAILY")
                                .minimumPreviousMonthSpent(Money.won(200_000))
                                .monthlyCap(Money.won(8_000))
                                .monthlyCountLimit(2)
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000))
                                )
                                .build(),
                        BenefitRule.percentage("NORI_KBPAY_OFFLINE", "KB Pay 오프라인 2% 추가 할인", 2)
                                .categories("ANY")
                                .requiredTags("KB_PAY", "OFFLINE")
                                .exclusiveGroup("NORI_KBPAY_BONUS")
                                .minimumPreviousMonthSpent(Money.won(300_000))
                                .monthlyCap(Money.won(3_000))
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000))
                                )
                                .build()
                )
        );

        var quote = policy.estimateBenefit(
                Money.won(15_000),
                "CGV 강남",
                "MOVIE",
                Set.of("KB_PAY", "OFFLINE"),
                Money.won(300_000),
                List.of(),
                List.of()
        );

        assertTrue(quote.isPresent());
        assertEquals(4_300L, quote.orElseThrow().benefitAmount().amount());
        assertTrue(quote.orElseThrow().summary().contains("영화 4,000원 할인"));
        assertTrue(quote.orElseThrow().summary().contains("KB Pay 오프라인 2% 추가 할인"));
    }

    @Test
    void capsKPassCoffeeBenefitByPreviousMonthTier() {
        CardPerformancePolicy policy = new CardPerformancePolicy(
                new CardId("SAMSUNG_KPASS"),
                List.of(new PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                List.of(
                        BenefitRule.percentage("KPASS_COFFEE", "커피전문점 20% 결제일할인", 20)
                                .categories("COFFEE")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                                        new BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000))
                                )
                                .build()
                )
        );

        var quote = policy.estimateBenefit(
                Money.won(50_000),
                "스타벅스 역삼",
                "COFFEE",
                Set.of(),
                Money.won(450_000),
                List.of(),
                List.of()
        );

        assertTrue(quote.isPresent());
        assertEquals(4_000L, quote.orElseThrow().benefitAmount().amount());
        assertTrue(quote.orElseThrow().wasCapped());
    }

    @Test
    void stopsMyWeshMovieBenefitAfterYearlyLimitIsConsumed() {
        CardPerformancePolicy policy = new CardPerformancePolicy(
                new CardId("KB_MY_WESH"),
                List.of(new PerformanceTier("WESH_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                List.of(
                        BenefitRule.percentage("WESH_MOVIE", "노는데 진심 영화관 30% 할인", 30)
                                .categories("MOVIE")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .perTransactionCap(Money.won(5_000))
                                .yearlyBenefitCap(Money.won(20_000))
                                .yearlyCountLimit(4)
                                .build()
                )
        );

        List<SpendingRecord> currentYearRecords = List.of(
                spendingRecord(LocalDate.of(2026, 1, 4), "CGV 홍대", "MOVIE"),
                spendingRecord(LocalDate.of(2026, 2, 7), "CGV 왕십리", "MOVIE"),
                spendingRecord(LocalDate.of(2026, 2, 21), "CGV 왕십리", "MOVIE"),
                spendingRecord(LocalDate.of(2026, 3, 8), "CGV 여의도", "MOVIE")
        );

        var quote = policy.estimateBenefit(
                Money.won(18_000),
                "CGV 용산",
                "MOVIE",
                Set.of(),
                Money.won(450_000),
                List.of(currentYearRecords.get(3)),
                currentYearRecords
        );

        assertTrue(quote.isEmpty());
    }

    private SpendingRecord spendingRecord(LocalDate spentOn, String merchantName, String merchantCategory) {
        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId("KB_MY_WESH"),
                Money.won(18_000),
                spentOn,
                merchantName,
                merchantCategory
        );
    }
}
