package com.jean202.cardmizer.api.recommendation;

import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.RecommendationCandidate;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.port.in.RecommendCardUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
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
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        NormalizedTransaction normalizedTransaction = transactionNormalizer.normalize(
                request.merchantName(),
                request.merchantCategory(),
                request.paymentTags()
        );
        RecommendationResult result = recommendCardUseCase.recommend(request.toDomain(normalizedTransaction));
        return RecommendationResponse.from(result);
    }

    public record RecommendationRequest(
            @NotBlank(message = "spendingMonth must not be blank")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "spendingMonth must match yyyy-MM")
            String spendingMonth,
            @Positive(message = "amount must be greater than 0")
            long amount,
            @NotBlank(message = "merchantName must not be blank")
            String merchantName,
            String merchantCategory,
            Set<@NotBlank(message = "paymentTags must not contain blank values") String> paymentTags
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
