package com.jean202.cardmizer.core.port.in;

import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;

public interface RecommendCardUseCase {
    RecommendationResult recommend(RecommendationContext context);
}
