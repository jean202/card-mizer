package com.jean202.cardmizer.core.port.out;

import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import java.util.List;

public interface LoadCardPerformancePoliciesPort {
    List<CardPerformancePolicy> loadAll();
}
