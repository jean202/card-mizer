package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.Card;
import java.util.List;

public interface GetCardsUseCase {
    List<Card> getAll();
}
