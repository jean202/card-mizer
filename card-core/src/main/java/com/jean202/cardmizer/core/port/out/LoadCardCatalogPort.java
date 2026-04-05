package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.Card;
import java.util.List;

public interface LoadCardCatalogPort {
    List<Card> loadAll();
}
