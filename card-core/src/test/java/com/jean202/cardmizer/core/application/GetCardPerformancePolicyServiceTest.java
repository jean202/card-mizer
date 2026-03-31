package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCardPerformancePolicyServiceTest {
    @Test
    void returnsPolicyForConfiguredCard() {
        CardId cardId = new CardId("SAMSUNG_KPASS");
        GetCardPerformancePolicyService service = new GetCardPerformancePolicyService(
                () -> List.of(new Card(cardId, "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1)),
                () -> List.of(new CardPerformancePolicy(
                        cardId,
                        List.of(new PerformanceTier("KPASS_40", Money.won(400_000), "전월 40만원 이상 혜택 구간"))
                ))
        );

        CardPerformancePolicy policy = service.get(cardId);

        assertEquals("SAMSUNG_KPASS", policy.cardId().value());
        assertEquals("KPASS_40", policy.highestTier().code());
    }

    @Test
    void rejectsUnknownCard() {
        GetCardPerformancePolicyService service = new GetCardPerformancePolicyService(
                List::of,
                List::of
        );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.get(new CardId("UNKNOWN_CARD"))
        );

        assertEquals("Card not found: UNKNOWN_CARD", exception.getMessage());
    }
}
