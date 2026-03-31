package com.jean202.cardmizer.api.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;
import com.jean202.cardmizer.core.port.in.RecommendCardUseCase;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationControllerTest {
    @Test
    void normalizesRequestBeforeDelegatingToUseCase() {
        CapturingRecommendCardUseCase useCase = new CapturingRecommendCardUseCase();
        RecommendationController controller = new RecommendationController(useCase, new TransactionNormalizer());

        controller.recommend(new RecommendationController.RecommendationRequest(
                "2026-03",
                15_000,
                "CGV 왕십리",
                null,
                Set.of("KB Pay")
        ));

        assertEquals("MOVIE", useCase.captured.merchantCategory());
        assertTrue(useCase.captured.paymentTags().contains("KB_PAY"));
        assertTrue(useCase.captured.paymentTags().contains("OFFLINE"));
        assertEquals("2026-03", useCase.captured.spendingPeriod().yearMonth().toString());
    }

    private static final class CapturingRecommendCardUseCase implements RecommendCardUseCase {
        private RecommendationContext captured;

        @Override
        public RecommendationResult recommend(RecommendationContext context) {
            this.captured = context;
            Card card = new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 1);
            return new RecommendationResult(card, "test", List.of());
        }
    }
}
