package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplaceCardPerformancePolicyServiceTest {
    @Test
    void replacesPolicyForConfiguredCard() {
        CapturingReplaceCardPerformancePolicyPort replacePort = new CapturingReplaceCardPerformancePolicyPort();
        CardId cardId = new CardId("SAMSUNG_KPASS");
        ReplaceCardPerformancePolicyService service = new ReplaceCardPerformancePolicyService(
                () -> List.of(new Card(cardId, "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)),
                replacePort
        );
        CardPerformancePolicy policy = new CardPerformancePolicy(
                cardId,
                List.of(new PerformanceTier("KPASS_50", Money.won(500_000), "전월 50만원 이상 혜택 구간"))
        );

        service.replace(policy);

        assertEquals(policy, replacePort.saved);
    }

    @Test
    void rejectsUnknownCard() {
        ReplaceCardPerformancePolicyService service = new ReplaceCardPerformancePolicyService(
                List::of,
                policy -> {
                }
        );
        CardPerformancePolicy policy = new CardPerformancePolicy(
                new CardId("UNKNOWN_CARD"),
                List.of(new PerformanceTier("DEFAULT", Money.ZERO, "기본 정책"))
        );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.replace(policy)
        );

        assertEquals("Card not found: UNKNOWN_CARD", exception.getMessage());
    }

    private static final class CapturingReplaceCardPerformancePolicyPort implements ReplaceCardPerformancePolicyPort {
        private CardPerformancePolicy saved;

        @Override
        public void replace(CardPerformancePolicy cardPerformancePolicy) {
            this.saved = cardPerformancePolicy;
        }
    }
}
