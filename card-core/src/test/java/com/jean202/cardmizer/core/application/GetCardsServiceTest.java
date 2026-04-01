package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCardsServiceTest {
    @Test
    void returnsAllCardsSortedByPriority() {
        Card highPriority = new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2", CardType.CHECK, 3);
        Card lowPriority = new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스", CardType.CREDIT, 1);
        Card midPriority = new Card(new CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH", CardType.CREDIT, 2);

        GetCardsService service = new GetCardsService(() -> List.of(highPriority, lowPriority, midPriority));

        List<Card> result = service.getAll();

        assertEquals(3, result.size());
        assertEquals("SAMSUNG_KPASS", result.get(0).id().value());
        assertEquals("KB_MY_WESH", result.get(1).id().value());
        assertEquals("KB_NORI2_KBPAY", result.get(2).id().value());
    }

    @Test
    void returnsEmptyListWhenNoCards() {
        GetCardsService service = new GetCardsService(List::of);

        List<Card> result = service.getAll();

        assertEquals(0, result.size());
    }
}
