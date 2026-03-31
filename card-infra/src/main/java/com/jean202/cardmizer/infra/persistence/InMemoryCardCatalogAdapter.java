package com.jean202.cardmizer.infra.persistence;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.SaveCardPort;
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryCardCatalogAdapter implements LoadCardCatalogPort, SaveCardPort, UpdateCardPriorityPort {
    private final CopyOnWriteArrayList<Card> cards = new CopyOnWriteArrayList<>(initialCards());

    @Override
    public List<Card> loadAll() {
        return cards.stream()
                .sorted(Comparator.comparingInt(Card::priority))
                .toList();
    }

    @Override
    public synchronized void save(Card card) {
        if (cards.stream().map(Card::id).anyMatch(card.id()::equals)) {
            throw new IllegalArgumentException("Card already exists: " + card.id().value());
        }

        List<Card> reorderedCards = new ArrayList<>();
        boolean inserted = false;
        int nextPriority = 1;

        for (Card existingCard : loadAll()) {
            if (!inserted && nextPriority == card.priority()) {
                reorderedCards.add(withPriority(card, nextPriority));
                inserted = true;
                nextPriority++;
            }

            reorderedCards.add(withPriority(existingCard, nextPriority));
            nextPriority++;
        }

        if (!inserted) {
            reorderedCards.add(withPriority(card, nextPriority));
        }

        cards.clear();
        cards.addAll(reorderedCards);
    }

    @Override
    public synchronized void update(PriorityStrategy priorityStrategy) {
        Map<CardId, Card> cardsById = loadAll().stream()
                .collect(Collectors.toMap(Card::id, Function.identity()));
        List<Card> reorderedCards = new ArrayList<>();

        int priority = 1;
        for (CardId orderedCardId : priorityStrategy.orderedCardIds()) {
            Card existingCard = cardsById.get(orderedCardId);
            if (existingCard == null) {
                throw new IllegalArgumentException("Unknown card id: " + orderedCardId.value());
            }
            reorderedCards.add(withPriority(existingCard, priority));
            priority++;
        }

        cards.clear();
        cards.addAll(reorderedCards);
    }

    private static List<Card> initialCards() {
        return List.of(
                new Card(new CardId("SAMSUNG_KPASS"), "삼성카드", "K-패스 삼성카드", CardType.CREDIT, 1),
                new Card(new CardId("KB_MY_WESH"), "KB국민카드", "My WE:SH KB국민카드", CardType.CREDIT, 2),
                new Card(new CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 3),
                new Card(
                        new CardId("HYUNDAI_ZERO_POINT"),
                        "현대카드",
                        "ZERO Edition2(포인트형)",
                        CardType.CREDIT,
                        4
                )
        );
    }

    private Card withPriority(Card card, int priority) {
        return new Card(
                card.id(),
                card.issuerName(),
                card.productName(),
                card.cardType(),
                priority
        );
    }
}
