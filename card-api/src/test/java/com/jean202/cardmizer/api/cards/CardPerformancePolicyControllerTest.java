package com.jean202.cardmizer.api.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier;
import com.jean202.cardmizer.core.domain.BenefitRule;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.in.ReplaceCardPerformancePolicyUseCase;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardPerformancePolicyControllerTest {
    @Test
    void returnsPolicyResponse() {
        CardPerformancePolicy policy = new CardPerformancePolicy(
                new CardId("SAMSUNG_KPASS"),
                List.of(new PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간")),
                List.of(
                        BenefitRule.percentage("KPASS_TRANSIT", "대중교통 10% 결제일할인", 10)
                                .categories("PUBLIC_TRANSIT")
                                .monthlyCapTiers(new BenefitMonthlyCapTier(Money.ZERO, Money.won(5_000)))
                                .build()
                )
        );
        CardPerformancePolicyController controller = new CardPerformancePolicyController(
                cardId -> policy,
                cardPerformancePolicy -> {
                }
        );

        CardPerformancePolicyController.CardPerformancePolicyResponse response = controller.get("SAMSUNG_KPASS");

        assertEquals("SAMSUNG_KPASS", response.cardId());
        assertEquals("KPASS_40", response.tiers().get(0).code());
        assertEquals("KPASS_TRANSIT", response.benefitRules().get(0).ruleId());
        assertEquals(1000, response.benefitRules().get(0).rateBasisPoints());
    }

    @Test
    void convertsReplaceRequestToDomainPolicy() {
        CapturingReplaceCardPerformancePolicyUseCase replaceUseCase = new CapturingReplaceCardPerformancePolicyUseCase();
        CardPerformancePolicyController controller = new CardPerformancePolicyController(
                cardId -> replaceUseCase.saved,
                replaceUseCase
        );

        CardPerformancePolicyController.CardPerformancePolicyResponse response = controller.replace(
                "SHINHAN_MR_LIFE",
                new CardPerformancePolicyController.ReplaceCardPerformancePolicyRequest(
                        List.of(new CardPerformancePolicyController.PerformanceTierRequest(
                                "LIFE_30",
                                300_000,
                                "전월 30만원 이상 혜택 구간"
                        )),
                        List.of(new CardPerformancePolicyController.BenefitRuleRequest(
                                "LIFE_OTT",
                                "OTT 10% 할인",
                                "RATE_PERCENT",
                                Set.of("OTT"),
                                Set.of(),
                                Set.of("SUBSCRIPTION"),
                                Set.of(),
                                "LIFE_PRIMARY",
                                null,
                                1000,
                                null,
                                0L,
                                0L,
                                300_000L,
                                List.of(new CardPerformancePolicyController.BenefitMonthlyCapTierRequest(0L, 10_000L)),
                                0L,
                                0,
                                0,
                                List.of(),
                                0L
                        ))
                )
        );

        assertEquals("SHINHAN_MR_LIFE", replaceUseCase.saved.cardId().value());
        assertEquals("LIFE_30", replaceUseCase.saved.highestTier().code());
        assertEquals("LIFE_OTT", replaceUseCase.saved.benefitRules().get(0).ruleId());
        assertEquals(1000, replaceUseCase.saved.benefitRules().get(0).rateBasisPoints());
        assertEquals("SHINHAN_MR_LIFE", response.cardId());
    }

    @Test
    void mergesPatchRequestWithCurrentPolicy() {
        CardPerformancePolicy currentPolicy = new CardPerformancePolicy(
                new CardId("SHINHAN_MR_LIFE"),
                List.of(new PerformanceTier("LIFE_30", Money.won(300_000), "전월 30만원 이상 혜택 구간")),
                List.of(BenefitRule.percentage("LIFE_OTT", "OTT 10% 할인", 10).categories("OTT").build())
        );
        CapturingReplaceCardPerformancePolicyUseCase replaceUseCase = new CapturingReplaceCardPerformancePolicyUseCase();
        CardPerformancePolicyController controller = new CardPerformancePolicyController(
                cardId -> currentPolicy,
                replaceUseCase
        );

        CardPerformancePolicyController.CardPerformancePolicyResponse response = controller.patch(
                "SHINHAN_MR_LIFE",
                new CardPerformancePolicyController.PatchCardPerformancePolicyRequest(
                        null,
                        List.of(new CardPerformancePolicyController.BenefitRuleRequest(
                                "LIFE_MART",
                                "마트 5천원 할인",
                                "FIXED_AMOUNT",
                                Set.of("MART"),
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                null,
                                null,
                                null,
                                5_000L,
                                0L,
                                0L,
                                0L,
                                List.of(),
                                0L,
                                0,
                                0,
                                List.of(),
                                0L
                        ))
                )
        );

        assertEquals("LIFE_30", replaceUseCase.saved.highestTier().code());
        assertEquals("LIFE_MART", replaceUseCase.saved.benefitRules().get(0).ruleId());
        assertEquals(5_000L, replaceUseCase.saved.benefitRules().get(0).fixedBenefitAmount().amount());
        assertEquals("SHINHAN_MR_LIFE", response.cardId());
        assertEquals("LIFE_30", response.tiers().get(0).code());
        assertEquals("LIFE_MART", response.benefitRules().get(0).ruleId());
    }

    private static final class CapturingReplaceCardPerformancePolicyUseCase implements ReplaceCardPerformancePolicyUseCase {
        private CardPerformancePolicy saved;

        @Override
        public void replace(CardPerformancePolicy cardPerformancePolicy) {
            this.saved = cardPerformancePolicy;
        }
    }
}
