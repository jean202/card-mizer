package com.jean202.cardmizer.api.recommendation;

import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.RecommendationCandidate;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.port.in.RecommendCardUseCase;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendCardUseCase recommendCardUseCase;
    private final TransactionNormalizer transactionNormalizer;

    public RecommendationController(
            RecommendCardUseCase recommendCardUseCase,
            TransactionNormalizer transactionNormalizer
    ) {
        this.recommendCardUseCase = recommendCardUseCase;
        this.transactionNormalizer = transactionNormalizer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public RecommendationResponse recommend(@RequestBody RecommendationRequest request) {
        NormalizedTransaction normalizedTransaction = transactionNormalizer.normalize(
                request.merchantName(),
                request.merchantCategory(),
                request.paymentTags()
        );
        RecommendationResult result = recommendCardUseCase.recommend(request.toDomain(normalizedTransaction));
        return RecommendationResponse.from(result);
    }

    public record RecommendationRequest(
            String spendingMonth,
            long amount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags
    ) {
        RecommendationContext toDomain(NormalizedTransaction normalizedTransaction) {
            return new RecommendationContext(
                    new SpendingPeriod(YearMonth.parse(spendingMonth)),
                    Money.won(amount),
                    merchantName,
                    normalizedTransaction.merchantCategory(),
                    normalizedTransaction.paymentTags()
            );
        }
    }

    public record RecommendationResponse(
            String recommendedCardId,
            String recommendedCardName,
            String reason,
            List<RecommendationAlternativeResponse> alternatives
    ) {
        static RecommendationResponse from(RecommendationResult result) {
            return new RecommendationResponse(
                    result.recommendedCard().id().value(),
                    result.recommendedCard().displayName(),
                    result.reason(),
                    result.alternatives().stream()
                            .map(RecommendationAlternativeResponse::from)
                            .toList()
            );
        }
    }

    public record RecommendationAlternativeResponse(
            String cardId,
            String cardName,
            String reason,
            int score
    ) {
        static RecommendationAlternativeResponse from(RecommendationCandidate candidate) {
            return new RecommendationAlternativeResponse(
                    candidate.card().id().value(),
                    candidate.card().displayName(),
                    candidate.reason(),
                    candidate.score()
            );
        }
    }
}
