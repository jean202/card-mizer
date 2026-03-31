package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.in.UpdatePriorityUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdatePriorityService implements UpdatePriorityUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final UpdateCardPriorityPort updateCardPriorityPort;

    public UpdatePriorityService(
            LoadCardCatalogPort loadCardCatalogPort,
            UpdateCardPriorityPort updateCardPriorityPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.updateCardPriorityPort = Objects.requireNonNull(
                updateCardPriorityPort,
                "updateCardPriorityPort must not be null"
        );
    }

    @Override
    public void update(PriorityStrategy priorityStrategy) {
        Objects.requireNonNull(priorityStrategy, "priorityStrategy must not be null");

        List<CardId> orderedCardIds = priorityStrategy.orderedCardIds();
        Set<CardId> uniqueOrderedCardIds = new LinkedHashSet<>(orderedCardIds);
        if (uniqueOrderedCardIds.size() != orderedCardIds.size()) {
            throw new IllegalArgumentException("Priority strategy must not contain duplicate card ids");
        }

        Set<CardId> configuredCardIds = loadCardCatalogPort.loadAll().stream()
                .map(Card::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!configuredCardIds.equals(uniqueOrderedCardIds)) {
            Set<CardId> missingCardIds = new LinkedHashSet<>(configuredCardIds);
            missingCardIds.removeAll(uniqueOrderedCardIds);

            Set<CardId> unknownCardIds = new LinkedHashSet<>(uniqueOrderedCardIds);
            unknownCardIds.removeAll(configuredCardIds);

            String detail = buildMismatchDetail(missingCardIds, unknownCardIds);
            throw new IllegalArgumentException("Priority strategy must include every configured card exactly once" + detail);
        }

        updateCardPriorityPort.update(priorityStrategy);
    }

    private String buildMismatchDetail(Set<CardId> missingCardIds, Set<CardId> unknownCardIds) {
        StringBuilder builder = new StringBuilder();
        if (!missingCardIds.isEmpty()) {
            builder.append(" (missing: ")
                    .append(formatCardIds(missingCardIds))
                    .append(')');
        }
        if (!unknownCardIds.isEmpty()) {
            builder.append(builder.isEmpty() ? " (" : " ")
                    .append("unknown: ")
                    .append(formatCardIds(unknownCardIds))
                    .append(')');
        }
        return builder.toString();
    }

    private String formatCardIds(Set<CardId> cardIds) {
        return cardIds.stream()
                .map(CardId::value)
                .collect(Collectors.joining(", "));
    }
}
