package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.PriorityStrategy;

public interface UpdateCardPriorityPort {
    void update(PriorityStrategy priorityStrategy);
}
