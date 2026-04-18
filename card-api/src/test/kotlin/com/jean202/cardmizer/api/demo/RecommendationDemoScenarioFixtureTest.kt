package com.jean202.cardmizer.api.demo

import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.application.RecommendCardService
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.RecommendationContext
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.infra.persistence.InMemoryCardCatalogAdapter
import com.jean202.cardmizer.infra.persistence.InMemoryCardPerformancePolicyAdapter
import com.jean202.cardmizer.infra.persistence.InMemorySpendingRecordAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class RecommendationDemoScenarioFixtureTest {
    private val transactionNormalizer = TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault())

    @Test
    fun loadedFixturesProduceExpectedRecommendations() {
        val scenarios = RecommendationDemoScenariosLoader().loadDefault()

        for (scenario in scenarios.scenarios) {
            val spendingRecordAdapter = InMemorySpendingRecordAdapter(
                scenario.seedRecords.map { toDomain(it) },
            )
            val service = RecommendCardService(
                InMemoryCardCatalogAdapter(),
                InMemoryCardPerformancePolicyAdapter(),
                spendingRecordAdapter,
            )

            val request = scenario.request
            val normalizedTransaction = transactionNormalizer.normalize(
                request.merchantName,
                request.merchantCategory,
                request.paymentTags,
            )

            val recommendedCardId = service.recommend(
                RecommendationContext(
                    SpendingPeriod(YearMonth.parse(request.spendingMonth)),
                    Money.won(request.amount),
                    request.merchantName,
                    normalizedTransaction.merchantCategory,
                    normalizedTransaction.paymentTags,
                ),
            ).recommendedCard.id.value

            assertEquals(scenario.expectedRecommendedCardId, recommendedCardId, scenario.id)
        }
    }

    private fun toDomain(fixture: DemoSpendingRecordFixture): SpendingRecord {
        val normalizedTransaction = transactionNormalizer.normalize(
            fixture.merchantName,
            fixture.merchantCategory,
            fixture.paymentTags,
        )
        return SpendingRecord(
            UUID.randomUUID(),
            CardId(fixture.cardId),
            Money.won(fixture.amount),
            fixture.spentOn,
            fixture.merchantName,
            normalizedTransaction.merchantCategory,
            normalizedTransaction.paymentTags,
        )
    }
}
