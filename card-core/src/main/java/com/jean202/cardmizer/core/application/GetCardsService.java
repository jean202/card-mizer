package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.port.in.GetCardsUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class GetCardsService implements GetCardsUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;

    public GetCardsService(LoadCardCatalogPort loadCardCatalogPort) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
    }

    @Override
    public List<Card> getAll() {
        return loadCardCatalogPort.loadAll().stream()
                .sorted(Comparator.comparingInt(Card::priority))
                .toList();
    }
}
