package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier;
import com.jean202.cardmizer.core.domain.BenefitRule;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendCardServiceTest {
    @Test
    void recommendsSamsungKPassWhenTransitBenefitAlsoCompletesThreshold() {
        Card samsung = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1);
        Card hyundai = new Card(new CardId("HYUNDAI_ZERO_POINT"), "현대카드", "ZERO Edition2(포인트형)", CardType.CREDIT, 4);

        RecommendCardService service = new RecommendCardService(
                () -> List.of(samsung, hyundai),
                () -> List.of(
                        new CardPerformancePolicy(
                                samsung.id(),
                                List.of(new PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                                List.of(BenefitRule.percentage("KPASS_TRANSIT", "대중교통 10% 결제일할인", 10)
                                        .categories("PUBLIC_TRANSIT")
                                        .minimumPreviousMonthSpent(Money.won(400_000))
                                        .monthlyCap(Money.won(5_000))
                                        .build())
                        ),
                        new CardPerformancePolicy(
                                hyundai.id(),
                                List.of(new PerformanceTier("ZERO_FREE", Money.ZERO, "실적 조건 없는 기본 적립")),
                                List.of(BenefitRule.percentage("ZERO_BASE", "국내외 가맹점 1% M포인트 적립", 1)
                                        .categories("ANY")
                                        .exclusiveGroup("ZERO_BASELINE")
                                        .build())
                        )
                ),
                period -> {
                    YearMonth yearMonth = period.yearMonth();
                    if (yearMonth.equals(YearMonth.of(2026, 2))) {
                        return List.of(
                                spendingRecord(samsung.id(), 450_000, LocalDate.of(2026, 2, 10), "생활비", "GENERAL_MERCHANT"),
                                spendingRecord(hyundai.id(), 150_000, LocalDate.of(2026, 2, 8), "생활비", "GENERAL_MERCHANT")
                        );
                    }
                    if (yearMonth.equals(YearMonth.of(2026, 3))) {
                        return List.of(
                                spendingRecord(samsung.id(), 380_000, LocalDate.of(2026, 3, 5), "교통비", "PUBLIC_TRANSIT"),
                                spendingRecord(hyundai.id(), 90_000, LocalDate.of(2026, 3, 7), "마트", "BIG_MART")
                        );
                    }
                    return List.of();
                }
        );

        RecommendationResult result = service.recommend(new RecommendationContext(
                new SpendingPeriod(YearMonth.of(2026, 3)),
                Money.won(20_000),
                "서울교통공사",
                "PUBLIC_TRANSIT"
        ));

        assertAll(
                () -> assertEquals("SAMSUNG_KPASS", result.recommendedCard().id().value()),
                () -> assertTrue(result.reason().contains("바로 달성")),
                () -> assertTrue(result.reason().contains("2,000원")),
                () -> assertEquals("HYUNDAI_ZERO_POINT", result.alternatives().get(0).card().id().value())
        );
    }

    @Test
    void prefersNoriWhenKbPayBonusStacksWithMovieDiscount() {
        Card nori = new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 3);
        Card samsung = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1);

        RecommendCardService service = new RecommendCardService(
                () -> List.of(nori, samsung),
                () -> List.of(
                        new CardPerformancePolicy(
                                nori.id(),
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
                                                .sharedMonthlyCapTiers(new BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)))
                                                .build(),
                                        BenefitRule.percentage("NORI_KBPAY_OFFLINE", "KB Pay 오프라인 2% 추가 할인", 2)
                                                .categories("ANY")
                                                .requiredTags("KB_PAY", "OFFLINE")
                                                .exclusiveGroup("NORI_KBPAY_BONUS")
                                                .minimumPreviousMonthSpent(Money.won(300_000))
                                                .monthlyCap(Money.won(3_000))
                                                .sharedLimitGroup("NORI_TOTAL")
                                                .sharedMonthlyCapTiers(new BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)))
                                                .build()
                                )
                        ),
                        new CardPerformancePolicy(
                                samsung.id(),
                                List.of(new PerformanceTier("KPASS_20", Money.won(200_000), "기준 구간"))
                        )
                ),
                period -> {
                    YearMonth yearMonth = period.yearMonth();
                    if (yearMonth.equals(YearMonth.of(2026, 2))) {
                        return List.of(
                                spendingRecord(nori.id(), 300_000, LocalDate.of(2026, 2, 10), "생활비", "GENERAL_MERCHANT"),
                                spendingRecord(samsung.id(), 100_000, LocalDate.of(2026, 2, 9), "생활비", "GENERAL_MERCHANT")
                        );
                    }
                    if (yearMonth.equals(YearMonth.of(2026, 3))) {
                        return List.of(
                                spendingRecord(nori.id(), 150_000, LocalDate.of(2026, 3, 5), "마트", "BIG_MART"),
                                spendingRecord(samsung.id(), 155_000, LocalDate.of(2026, 3, 7), "쇼핑", "ONLINE_SHOPPING")
                        );
                    }
                    return List.of();
                }
        );

        RecommendationResult result = service.recommend(new RecommendationContext(
                new SpendingPeriod(YearMonth.of(2026, 3)),
                Money.won(15_000),
                "CGV 왕십리",
                "MOVIE",
                Set.of("KB_PAY", "OFFLINE")
        ));

        assertAll(
                () -> assertEquals("KB_NORI2_KBPAY", result.recommendedCard().id().value()),
                () -> assertTrue(result.reason().contains("4,300원")),
                () -> assertEquals("SAMSUNG_KPASS", result.alternatives().get(0).card().id().value())
        );
    }

    private SpendingRecord spendingRecord(CardId cardId, long amount, LocalDate spentOn, String merchantName, String merchantCategory) {
        return new SpendingRecord(
                UUID.randomUUID(),
                cardId,
                Money.won(amount),
                spentOn,
                merchantName,
                merchantCategory
        );
    }
}
