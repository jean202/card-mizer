package com.jean202.cardmizer.infra.persistence.jpa;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.SaveCardPort;
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Transactional
public class JpaCardCatalogAdapter implements LoadCardCatalogPort, SaveCardPort, UpdateCardPriorityPort {
    private final JpaCardRepository repository;

    public JpaCardCatalogAdapter(JpaCardRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> loadAll() {
        return repository.findAllByOrderByPriorityAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(Card card) {
        List<Card> reorderedCards = new ArrayList<>();
        List<Card> existingCards = loadAll();
        boolean inserted = false;
        int nextPriority = 1;

        for (Card existingCard : existingCards) {
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

        repository.saveAll(reorderedCards.stream().map(this::toEntity).toList());
    }

    @Override
    public void update(PriorityStrategy priorityStrategy) {
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

        repository.saveAll(reorderedCards.stream().map(this::toEntity).toList());
    }

    private Card toDomain(JpaCardEntity entity) {
        return new Card(
                new CardId(entity.getId()),
                entity.getIssuerName(),
                entity.getProductName(),
                CardType.valueOf(entity.getCardType()),
                entity.getPriority()
        );
    }

    private JpaCardEntity toEntity(Card card) {
        return new JpaCardEntity(
                card.id().value(),
                card.issuerName(),
                card.productName(),
                card.cardType().name(),
                card.priority()
        );
    }

    private Card withPriority(Card card, int priority) {
        return new Card(card.id(), card.issuerName(), card.productName(), card.cardType(), priority);
    }
}
