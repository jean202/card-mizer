package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.in.RegisterCardUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPort;
import java.util.List;
import java.util.Objects;

public class RegisterCardService implements RegisterCardUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final SaveCardPort saveCardPort;
    private final SaveCardPerformancePolicyPort saveCardPerformancePolicyPort;

    public RegisterCardService(
            LoadCardCatalogPort loadCardCatalogPort,
            SaveCardPort saveCardPort,
            SaveCardPerformancePolicyPort saveCardPerformancePolicyPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.saveCardPort = Objects.requireNonNull(saveCardPort, "saveCardPort must not be null");
        this.saveCardPerformancePolicyPort = Objects.requireNonNull(
                saveCardPerformancePolicyPort,
                "saveCardPerformancePolicyPort must not be null"
        );
    }

    @Override
    public void register(Card card) {
        Objects.requireNonNull(card, "card must not be null");

        List<Card> existingCards = loadCardCatalogPort.loadAll();
        boolean duplicateCardId = existingCards.stream()
                .map(Card::id)
                .anyMatch(card.id()::equals);
        if (duplicateCardId) {
            throw new IllegalArgumentException("Card already exists: " + card.id().value());
        }

        int maxAllowedPriority = existingCards.size() + 1;
        if (card.priority() > maxAllowedPriority) {
            throw new IllegalArgumentException("Priority must be between 1 and " + maxAllowedPriority);
        }

        saveCardPort.save(card);
        saveCardPerformancePolicyPort.save(defaultPolicyFor(card));
    }

    private CardPerformancePolicy defaultPolicyFor(Card card) {
        return new CardPerformancePolicy(
                card.id(),
                List.of(new PerformanceTier("DEFAULT_BASE", Money.ZERO, "실적 조건 미설정"))
        );
    }
}
