package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisterCardServiceTest {
    @Test
    void savesCardAndDefaultPolicy() {
        Card existingCard = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1);
        CapturingSaveCardPort saveCardPort = new CapturingSaveCardPort();
        CapturingSaveCardPerformancePolicyPort saveCardPerformancePolicyPort = new CapturingSaveCardPerformancePolicyPort();
        RegisterCardService service = new RegisterCardService(
                () -> List.of(existingCard),
                saveCardPort,
                saveCardPerformancePolicyPort
        );

        Card newCard = new Card(new CardId("SHINHAN_MR_LIFE"), "신한카드", "Mr.Life", CardType.CREDIT, 2);

        service.register(newCard);

        assertEquals(newCard, saveCardPort.saved);
        assertEquals("SHINHAN_MR_LIFE", saveCardPerformancePolicyPort.saved.cardId().value());
        assertEquals("DEFAULT_BASE", saveCardPerformancePolicyPort.saved.highestTier().code());
    }

    @Test
    void rejectsDuplicateCardId() {
        Card existingCard = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1);
        RegisterCardService service = new RegisterCardService(
                () -> List.of(existingCard),
                card -> {
                },
                policy -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "다른 카드", CardType.CREDIT, 1))
        );

        assertEquals("Card already exists: SAMSUNG_KPASS", exception.getMessage());
    }

    @Test
    void rejectsPriorityOutsideInsertRange() {
        RegisterCardService service = new RegisterCardService(
                existingCards(),
                card -> {
                },
                policy -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(new Card(new CardId("SHINHAN_MR_LIFE"), "신한카드", "Mr.Life", CardType.CREDIT, 4))
        );

        assertEquals("Priority must be between 1 and 3", exception.getMessage());
    }

    private LoadCardCatalogPort existingCards() {
        return () -> List.of(
                new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
                new Card(new CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH", CardType.CREDIT, 2)
        );
    }

    private static final class CapturingSaveCardPort implements SaveCardPort {
        private Card saved;

        @Override
        public void save(Card card) {
            this.saved = card;
        }
    }

    private static final class CapturingSaveCardPerformancePolicyPort implements SaveCardPerformancePolicyPort {
        private CardPerformancePolicy saved;

        @Override
        public void save(CardPerformancePolicy cardPerformancePolicy) {
            this.saved = cardPerformancePolicy;
        }
    }
}
