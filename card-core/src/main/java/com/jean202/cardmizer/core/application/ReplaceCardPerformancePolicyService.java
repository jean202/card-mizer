package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.port.in.ReplaceCardPerformancePolicyUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort;
import java.util.Objects;

public class ReplaceCardPerformancePolicyService implements ReplaceCardPerformancePolicyUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final ReplaceCardPerformancePolicyPort replaceCardPerformancePolicyPort;

    public ReplaceCardPerformancePolicyService(
            LoadCardCatalogPort loadCardCatalogPort,
            ReplaceCardPerformancePolicyPort replaceCardPerformancePolicyPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.replaceCardPerformancePolicyPort = Objects.requireNonNull(
                replaceCardPerformancePolicyPort,
                "replaceCardPerformancePolicyPort must not be null"
        );
    }

    @Override
    public void replace(CardPerformancePolicy cardPerformancePolicy) {
        Objects.requireNonNull(cardPerformancePolicy, "cardPerformancePolicy must not be null");

        if (loadCardCatalogPort.loadAll().stream().map(Card::id).noneMatch(cardPerformancePolicy.cardId()::equals)) {
            throw new ResourceNotFoundException("Card not found: " + cardPerformancePolicy.cardId().value());
        }

        replaceCardPerformancePolicyPort.replace(cardPerformancePolicy);
    }
}
