package com.jean202.cardmizer.api.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.in.RegisterCardUseCase;
import com.jean202.cardmizer.core.port.in.UpdatePriorityUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardManagementControllerTest {
    @Test
    void convertsRegisterRequestToCardDomainModel() {
        CapturingRegisterCardUseCase registerCardUseCase = new CapturingRegisterCardUseCase();
        CardManagementController controller = new CardManagementController(List::of, registerCardUseCase, priorityStrategy -> {
        });

        CardManagementController.RegisterCardResponse response = controller.register(
                new CardManagementController.RegisterCardRequest(
                        "SHINHAN_MR_LIFE",
                        "신한카드",
                        "Mr.Life",
                        "credit",
                        2
                )
        );

        assertEquals("SHINHAN_MR_LIFE", registerCardUseCase.saved.id().value());
        assertEquals("신한카드 Mr.Life", response.cardName());
        assertEquals("CREDIT", response.cardType());
        assertEquals(2, response.priority());
    }

    @Test
    void convertsPriorityRequestToOrderedCardIds() {
        CapturingUpdatePriorityUseCase updatePriorityUseCase = new CapturingUpdatePriorityUseCase();
        CardManagementController controller = new CardManagementController(List::of, card -> {
        }, updatePriorityUseCase);

        controller.updatePriorities(new CardManagementController.UpdatePrioritiesRequest(List.of(
                "KB_NORI2_KBPAY",
                "SAMSUNG_KPASS"
        )));

        assertEquals(List.of("KB_NORI2_KBPAY", "SAMSUNG_KPASS"), updatePriorityUseCase.saved.orderedCardIds().stream()
                .map(cardId -> cardId.value())
                .toList());
    }

    private static final class CapturingRegisterCardUseCase implements RegisterCardUseCase {
        private Card saved;

        @Override
        public void register(Card card) {
            this.saved = card;
        }
    }

    private static final class CapturingUpdatePriorityUseCase implements UpdatePriorityUseCase {
        private PriorityStrategy saved;

        @Override
        public void update(PriorityStrategy priorityStrategy) {
            this.saved = priorityStrategy;
        }
    }
}
