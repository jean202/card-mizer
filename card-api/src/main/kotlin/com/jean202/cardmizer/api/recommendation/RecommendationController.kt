package com.jean202.cardmizer.api.recommendation

import com.jean202.cardmizer.api.normalization.NormalizedTransaction
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.RecommendationCandidate
import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.RecommendationResult
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.port.`in`.RecommendCardUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendCardUseCase: RecommendCardUseCase,
    private val transactionNormalizer: TransactionNormalizer,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun recommend(@Valid @RequestBody request: RecommendationRequest): RecommendationResponse {
        val normalized = transactionNormalizer.normalize(request.merchantName, request.merchantCategory, request.paymentTags)
        val result = recommendCardUseCase.recommend(request.toDomain(normalized))
        return RecommendationResponse.from(result)
    }

    data class RecommendationRequest(
        @field:NotBlank(message = "spendingMonth must not be blank")
        @field:Pattern(regexp = "^\\d{4}-\\d{2}$", message = "spendingMonth must match yyyy-MM")
        val spendingMonth: String,
        @field:Positive(message = "amount must be greater than 0")
        val amount: Long,
        @field:NotBlank(message = "merchantName must not be blank")
        val merchantName: String,
        val merchantCategory: String?,
        val paymentTags: Set<@NotBlank(message = "paymentTags must not contain blank values") String>?,
    ) {
        fun toDomain(normalized: NormalizedTransaction) = RecommendationContext(
            spendingPeriod = SpendingPeriod(YearMonth.parse(spendingMonth)),
            amount = Money.won(amount),
            merchantName = merchantName,
            merchantCategory = normalized.merchantCategory,
            paymentTags = normalized.paymentTags,
        )
    }

    data class RecommendationResponse(
        val recommendedCardId: String,
        val recommendedCardName: String,
        val reason: String,
        val alternatives: List<RecommendationAlternativeResponse>,
    ) {
        companion object {
            fun from(result: RecommendationResult) = RecommendationResponse(
                recommendedCardId = result.recommendedCard.id.value,
                recommendedCardName = result.recommendedCard.displayName(),
                reason = result.reason,
                alternatives = result.alternatives.map { RecommendationAlternativeResponse.from(it) },
            )
        }
    }

    data class RecommendationAlternativeResponse(
        val cardId: String,
        val cardName: String,
        val reason: String,
        val score: Int,
    ) {
        companion object {
            fun from(candidate: RecommendationCandidate) = RecommendationAlternativeResponse(
                cardId = candidate.card.id.value,
                cardName = candidate.card.displayName(),
                reason = candidate.reason,
                score = candidate.score,
            )
        }
    }
}
