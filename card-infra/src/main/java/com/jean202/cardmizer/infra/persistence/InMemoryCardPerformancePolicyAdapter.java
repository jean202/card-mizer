package com.jean202.cardmizer.infra.persistence;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier;
import com.jean202.cardmizer.core.domain.BenefitRule;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCardPerformancePolicyAdapter implements
        LoadCardPerformancePoliciesPort,
        SaveCardPerformancePolicyPort,
        ReplaceCardPerformancePolicyPort {
    private final CopyOnWriteArrayList<CardPerformancePolicy> policies = new CopyOnWriteArrayList<>(initialPolicies());

    @Override
    public List<CardPerformancePolicy> loadAll() {
        return List.copyOf(policies);
    }

    @Override
    public void save(CardPerformancePolicy cardPerformancePolicy) {
        boolean duplicatePolicy = policies.stream()
                .map(CardPerformancePolicy::cardId)
                .anyMatch(cardPerformancePolicy.cardId()::equals);
        if (duplicatePolicy) {
            throw new IllegalArgumentException("Card policy already exists: " + cardPerformancePolicy.cardId().value());
        }
        policies.add(cardPerformancePolicy);
    }

    @Override
    public synchronized void replace(CardPerformancePolicy cardPerformancePolicy) {
        for (int index = 0; index < policies.size(); index++) {
            if (policies.get(index).cardId().equals(cardPerformancePolicy.cardId())) {
                policies.set(index, cardPerformancePolicy);
                return;
            }
        }
        policies.add(cardPerformancePolicy);
    }

    private List<CardPerformancePolicy> initialPolicies() {
        return List.of(
                kPassSamsungPolicy(),
                myWeshPolicy(),
                nori2KbPayPolicy(),
                hyundaiZeroPointPolicy()
        );
    }

    private CardPerformancePolicy kPassSamsungPolicy() {
        return new CardPerformancePolicy(
                new CardId("SAMSUNG_KPASS"),
                List.of(
                        new PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간"),
                        new PerformanceTier("KPASS_80", Money.won(800_000), "전월 80만원 이상 혜택 구간")
                ),
                List.of(
                        BenefitRule.percentage("KPASS_TRANSIT", "대중교통 10% 결제일할인", 10)
                                .categories("PUBLIC_TRANSIT")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(5_000)),
                                        new BenefitMonthlyCapTier(Money.won(800_000), Money.won(10_000))
                                )
                                .build(),
                        BenefitRule.percentage("KPASS_COFFEE", "커피전문점 20% 결제일할인", 20)
                                .categories("COFFEE")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                                        new BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000))
                                )
                                .build(),
                        BenefitRule.percentage("KPASS_DIGITAL", "디지털콘텐츠/멤버십 20% 결제일할인", 20)
                                .categories("DIGITAL_CONTENT", "MEMBERSHIP", "OTT")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                                        new BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000))
                                )
                                .build(),
                        BenefitRule.percentage("KPASS_ONLINE", "온라인쇼핑 3% 결제일할인", 3)
                                .categories("ONLINE_SHOPPING", "ONLINE_FASHION")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCapTiers(
                                        new BenefitMonthlyCapTier(Money.won(400_000), Money.won(4_000)),
                                        new BenefitMonthlyCapTier(Money.won(800_000), Money.won(8_000))
                                )
                                .build()
                )
        );
    }

    private CardPerformancePolicy myWeshPolicy() {
        return new CardPerformancePolicy(
                new CardId("KB_MY_WESH"),
                List.of(new PerformanceTier("WESH_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                List.of(
                        BenefitRule.percentage("WESH_KB_PAY", "KB Pay 10% 할인", 10)
                                .categories("ANY")
                                .requiredTags("KB_PAY")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .perTransactionCap(Money.won(2_500))
                                .monthlyCap(Money.won(5_000))
                                .build(),
                        BenefitRule.percentage("WESH_FOOD", "음식점/편의점 10% 할인", 10)
                                .categories("RESTAURANT", "CONVENIENCE_STORE")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .perTransactionCap(Money.won(2_500))
                                .monthlyCap(Money.won(5_000))
                                .build(),
                        BenefitRule.percentage("WESH_MOBILE", "이동통신요금 10% 할인", 10)
                                .categories("MOBILE_BILL")
                                .requiredTags("AUTO_BILL")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCap(Money.won(5_000))
                                .build(),
                        BenefitRule.percentage("WESH_OTT", "OTT 30% 할인", 30)
                                .categories("OTT")
                                .requiredTags("SUBSCRIPTION")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCap(Money.won(5_000))
                                .build(),
                        BenefitRule.percentage("WESH_PLAY", "노는데 진심 택시/커피 5% 할인", 5)
                                .categories("TAXI", "COFFEE")
                                .exclusiveGroup("WESH_PRIMARY")
                                .minimumPreviousMonthSpent(Money.won(400_000))
                                .monthlyCap(Money.won(5_000))
                                .build(),
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
    }

    private CardPerformancePolicy nori2KbPayPolicy() {
        BenefitMonthlyCapTier[] noriSharedCapTiers = new BenefitMonthlyCapTier[] {
                new BenefitMonthlyCapTier(Money.ZERO, Money.won(20_000)),
                new BenefitMonthlyCapTier(Money.won(400_000), Money.won(30_000)),
                new BenefitMonthlyCapTier(Money.won(600_000), Money.won(40_000)),
                new BenefitMonthlyCapTier(Money.won(800_000), Money.won(50_000))
        };

        return new CardPerformancePolicy(
                new CardId("KB_NORI2_KBPAY"),
                List.of(
                        new PerformanceTier("NORI_20", Money.won(200_000), "전월 20만원 이상 혜택 구간"),
                        new PerformanceTier("NORI_40", Money.won(400_000), "전월 40만원 이상 혜택 구간"),
                        new PerformanceTier("NORI_60", Money.won(600_000), "전월 60만원 이상 혜택 구간"),
                        new PerformanceTier("NORI_80", Money.won(800_000), "전월 80만원 이상 혜택 구간")
                ),
                List.of(
                        BenefitRule.percentage("NORI_COFFEE", "커피 10% 할인", 10)
                                .categories("COFFEE")
                                .exclusiveGroup("NORI_DAILY")
                                .monthlyCap(Money.won(3_000))
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build(),
                        BenefitRule.percentage("NORI_CULTURE", "문화 10% 할인", 10)
                                .categories("CULTURE")
                                .merchantKeywords("INTERPARK", "인터파크")
                                .exclusiveGroup("NORI_DAILY")
                                .minimumPreviousMonthSpent(Money.won(200_000))
                                .monthlyCap(Money.won(7_000))
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build(),
                        BenefitRule.fixedAmount("NORI_DELIVERY", "배달 1,000원 할인", Money.won(1_000))
                                .categories("FOOD_DELIVERY")
                                .merchantKeywords("배달의민족", "요기요")
                                .exclusiveGroup("NORI_DAILY")
                                .minimumPreviousMonthSpent(Money.won(200_000))
                                .monthlyCap(Money.won(1_000))
                                .monthlyCountLimit(1)
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build(),
                        BenefitRule.fixedAmount("NORI_MOVIE", "영화 4,000원 할인", Money.won(4_000))
                                .categories("MOVIE")
                                .merchantKeywords("CGV")
                                .exclusiveGroup("NORI_DAILY")
                                .minimumPreviousMonthSpent(Money.won(200_000))
                                .monthlyCap(Money.won(8_000))
                                .monthlyCountLimit(2)
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build(),
                        BenefitRule.percentage("NORI_KBPAY_OFFLINE", "KB Pay 오프라인 2% 추가 할인", 2)
                                .categories("ANY")
                                .requiredTags("KB_PAY", "OFFLINE")
                                .exclusiveGroup("NORI_KBPAY_BONUS")
                                .minimumPreviousMonthSpent(Money.won(300_000))
                                .monthlyCap(Money.won(3_000))
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build(),
                        BenefitRule.percentage("NORI_KBPAY_ONLINE", "KB Pay 온라인 2% 추가 할인", 2)
                                .categories("ANY")
                                .requiredTags("KB_PAY", "ONLINE")
                                .exclusiveGroup("NORI_KBPAY_BONUS")
                                .minimumPreviousMonthSpent(Money.won(300_000))
                                .monthlyCap(Money.won(2_000))
                                .sharedLimitGroup("NORI_TOTAL")
                                .sharedMonthlyCapTiers(noriSharedCapTiers)
                                .build()
                )
        );
    }

    private CardPerformancePolicy hyundaiZeroPointPolicy() {
        return new CardPerformancePolicy(
                new CardId("HYUNDAI_ZERO_POINT"),
                List.of(new PerformanceTier("ZERO_FREE", Money.ZERO, "실적 조건 없는 기본 적립")),
                List.of(
                        BenefitRule.percentage("ZERO_BASE", "국내외 가맹점 1% M포인트 적립", 1)
                                .categories("ANY")
                                .exclusiveGroup("ZERO_BASELINE")
                                .build(),
                        BenefitRule.percentage("ZERO_LIFESTYLE", "생활 필수 영역 2.5% M포인트 적립", 2)
                                .categories("BIG_MART", "CONVENIENCE_STORE", "RESTAURANT", "COFFEE", "PUBLIC_TRANSIT")
                                .exclusiveGroup("ZERO_BASELINE")
                                .rateBasisPoints(250)
                                .build(),
                        BenefitRule.percentage("ZERO_SIMPLE_PAY", "온라인 간편결제 2.5% M포인트 적립", 2)
                                .categories("ANY")
                                .requiredTags("SIMPLE_PAY_ONLINE")
                                .exclusiveGroup("ZERO_BASELINE")
                                .rateBasisPoints(250)
                                .build()
                )
        );
    }
}
