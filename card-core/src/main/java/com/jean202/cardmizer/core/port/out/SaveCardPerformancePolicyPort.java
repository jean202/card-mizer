package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.CardPerformancePolicy;

public interface SaveCardPerformancePolicyPort {
    void save(CardPerformancePolicy cardPerformancePolicy);
}
