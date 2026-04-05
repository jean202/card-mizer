package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.Card;

public interface SaveCardPort {
    void save(Card card);
}
