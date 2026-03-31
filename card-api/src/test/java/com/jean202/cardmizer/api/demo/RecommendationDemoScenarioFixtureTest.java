package com.jean202.cardmizer.api.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.application.RecommendCardService;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.infra.persistence.InMemoryCardCatalogAdapter;
import com.jean202.cardmizer.infra.persistence.InMemoryCardPerformancePolicyAdapter;
import com.jean202.cardmizer.infra.persistence.InMemorySpendingRecordAdapter;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationDemoScenarioFixtureTest {
    private final TransactionNormalizer transactionNormalizer = new TransactionNormalizer();

    @Test
    void loadedFixturesProduceExpectedRecommendations() {
        RecommendationDemoScenarios scenarios = new RecommendationDemoScenariosLoader().loadDefault();

        for (RecommendationDemoScenario scenario : scenarios.scenarios()) {
            InMemorySpendingRecordAdapter spendingRecordAdapter = new InMemorySpendingRecordAdapter(
                    scenario.seedRecords().stream()
                            .map(this::toDomain)
                            .toList()
            );
            RecommendCardService service = new RecommendCardService(
                    new InMemoryCardCatalogAdapter(),
                    new InMemoryCardPerformancePolicyAdapter(),
                    spendingRecordAdapter
            );

            DemoRecommendationRequest request = scenario.request();
            NormalizedTransaction normalizedTransaction = transactionNormalizer.normalize(
                    request.merchantName(),
                    request.merchantCategory(),
                    request.paymentTags()
            );

            String recommendedCardId = service.recommend(new RecommendationContext(
                    new SpendingPeriod(YearMonth.parse(request.spendingMonth())),
                    Money.won(request.amount()),
                    request.merchantName(),
                    normalizedTransaction.merchantCategory(),
                    normalizedTransaction.paymentTags()
            )).recommendedCard().id().value();

            assertEquals(scenario.expectedRecommendedCardId(), recommendedCardId, scenario.id());
        }
    }

    private SpendingRecord toDomain(DemoSpendingRecordFixture fixture) {
        NormalizedTransaction normalizedTransaction = transactionNormalizer.normalize(
                fixture.merchantName(),
                fixture.merchantCategory(),
                fixture.paymentTags()
        );
        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId(fixture.cardId()),
                Money.won(fixture.amount()),
                fixture.spentOn(),
                fixture.merchantName(),
                normalizedTransaction.merchantCategory(),
                normalizedTransaction.paymentTags()
        );
    }
}
