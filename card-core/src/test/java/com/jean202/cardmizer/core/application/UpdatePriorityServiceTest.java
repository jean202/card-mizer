package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpdatePriorityServiceTest {
    @Test
    void updatesPriorityWhenStrategyMatchesConfiguredCards() {
        CapturingUpdateCardPriorityPort updateCardPriorityPort = new CapturingUpdateCardPriorityPort();
        UpdatePriorityService service = new UpdatePriorityService(
                this::configuredCards,
                updateCardPriorityPort
        );
        PriorityStrategy strategy = new PriorityStrategy(List.of(
                new CardId("KB_NORI2_KBPAY"),
                new CardId("SAMSUNG_KPASS")
        ));

        service.update(strategy);

        assertEquals(strategy, updateCardPriorityPort.saved);
    }

    @Test
    void rejectsDuplicateCardIds() {
        UpdatePriorityService service = new UpdatePriorityService(
                this::configuredCards,
                priorityStrategy -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.update(new PriorityStrategy(List.of(
                        new CardId("SAMSUNG_KPASS"),
                        new CardId("SAMSUNG_KPASS")
                )))
        );

        assertEquals("Priority strategy must not contain duplicate card ids", exception.getMessage());
    }

    @Test
    void rejectsMissingOrUnknownCards() {
        UpdatePriorityService service = new UpdatePriorityService(
                this::configuredCards,
                priorityStrategy -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.update(new PriorityStrategy(List.of(
                        new CardId("SAMSUNG_KPASS"),
                        new CardId("UNKNOWN")
                )))
        );

        assertEquals(
                "Priority strategy must include every configured card exactly once (missing: KB_NORI2_KBPAY) unknown: UNKNOWN)",
                exception.getMessage()
        );
    }

    private List<Card> configuredCards() {
        return List.of(
                new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
                new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 2)
        );
    }

    private static final class CapturingUpdateCardPriorityPort implements UpdateCardPriorityPort {
        private PriorityStrategy saved;

        @Override
        public void update(PriorityStrategy priorityStrategy) {
            this.saved = priorityStrategy;
        }
    }
}
