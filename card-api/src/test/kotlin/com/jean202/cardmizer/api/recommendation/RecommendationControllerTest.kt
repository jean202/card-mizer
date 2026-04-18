package com.jean202.cardmizer.api.recommendation

import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.RecommendationResult
import com.jean202.cardmizer.core.port.`in`.RecommendCardUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecommendationControllerTest {
    @Test
    fun normalizesRequestBeforeDelegatingToUseCase() {
        val useCase = CapturingRecommendCardUseCase()
        val controller = RecommendationController(useCase, TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault()))

        controller.recommend(
            RecommendationController.RecommendationRequest(
                spendingMonth = "2026-03",
                amount = 15_000,
                merchantName = "CGV 왕십리",
                merchantCategory = null,
                paymentTags = setOf("KB Pay"),
            ),
        )

        assertEquals("MOVIE", useCase.captured!!.merchantCategory)
        assertTrue("KB_PAY" in useCase.captured!!.paymentTags)
        assertTrue("OFFLINE" in useCase.captured!!.paymentTags)
        assertEquals("2026-03", useCase.captured!!.spendingPeriod.yearMonth.toString())
    }

    private class CapturingRecommendCardUseCase : RecommendCardUseCase {
        var captured: RecommendationContext? = null

        override fun recommend(context: RecommendationContext): RecommendationResult {
            captured = context
            val card = Card(CardId("KB_NORI2_KBPAY"), "KB국민카드", "노리2 체크카드(KB Pay)", CardType.CHECK, 1)
            return RecommendationResult(card, "test", emptyList())
        }
    }
}
