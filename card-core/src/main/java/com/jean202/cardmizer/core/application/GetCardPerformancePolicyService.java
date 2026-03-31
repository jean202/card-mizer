package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.port.in.GetCardPerformancePolicyUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import java.util.Objects;

public class GetCardPerformancePolicyService implements GetCardPerformancePolicyUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort;

    public GetCardPerformancePolicyService(
            LoadCardCatalogPort loadCardCatalogPort,
            LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.loadCardPerformancePoliciesPort = Objects.requireNonNull(
                loadCardPerformancePoliciesPort,
                "loadCardPerformancePoliciesPort must not be null"
        );
    }

    @Override
    public CardPerformancePolicy get(CardId cardId) {
        Objects.requireNonNull(cardId, "cardId must not be null");

        if (loadCardCatalogPort.loadAll().stream().map(Card::id).noneMatch(cardId::equals)) {
            throw new ResourceNotFoundException("Card not found: " + cardId.value());
        }

        return loadCardPerformancePoliciesPort.loadAll().stream()
                .filter(policy -> policy.cardId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Card performance policy not found: " + cardId.value()));
    }
}
