package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;

public interface GetCardPerformancePolicyUseCase {
    CardPerformancePolicy get(CardId cardId);
}
