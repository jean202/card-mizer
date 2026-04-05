package com.jean202.cardmizer.api.config;

import com.jean202.cardmizer.api.demo.RecommendationDemoScenarios;
import com.jean202.cardmizer.api.demo.RecommendationDemoScenariosLoader;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader;
import com.jean202.cardmizer.api.normalization.MerchantNormalizationRules;
import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.application.DeleteSpendingRecordService;
import com.jean202.cardmizer.core.application.GetCardsService;
import com.jean202.cardmizer.core.application.GetCardPerformancePolicyService;
import com.jean202.cardmizer.core.application.GetSpendingRecordsService;
import com.jean202.cardmizer.core.application.SyncCardTransactionsService;
import com.jean202.cardmizer.core.application.GetPerformanceOverviewService;
import com.jean202.cardmizer.core.application.RecommendCardService;
import com.jean202.cardmizer.core.application.ReplaceCardPerformancePolicyService;
import com.jean202.cardmizer.core.application.RegisterCardService;
import com.jean202.cardmizer.core.application.RecordSpendingService;
import com.jean202.cardmizer.core.application.UpdatePriorityService;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.DeleteSpendingRecordUseCase;
import com.jean202.cardmizer.core.port.in.GetCardsUseCase;
import com.jean202.cardmizer.core.port.in.GetCardPerformancePolicyUseCase;
import com.jean202.cardmizer.core.port.in.GetPerformanceOverviewUseCase;
import com.jean202.cardmizer.core.port.in.GetSpendingRecordsUseCase;
import com.jean202.cardmizer.core.port.in.RecommendCardUseCase;
import com.jean202.cardmizer.core.port.in.ReplaceCardPerformancePolicyUseCase;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import com.jean202.cardmizer.core.port.in.RegisterCardUseCase;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase;
import com.jean202.cardmizer.core.port.in.UpdatePriorityUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPort;
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort;
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort;
import com.jean202.cardmizer.infra.persistence.InMemoryCardCatalogAdapter;
import com.jean202.cardmizer.infra.persistence.InMemoryCardPerformancePolicyAdapter;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public MerchantNormalizationRules merchantNormalizationRules() {
        return new MerchantNormalizationRulesLoader().loadDefault();
    }

    @Bean
    public TransactionNormalizer transactionNormalizer(MerchantNormalizationRules merchantNormalizationRules) {
        return new TransactionNormalizer(merchantNormalizationRules);
    }

    @Bean
    public RecommendationDemoScenarios recommendationDemoScenarios() {
        return new RecommendationDemoScenariosLoader().loadDefault();
    }

    @Bean
    public GetCardsUseCase getCardsUseCase(LoadCardCatalogPort loadCardCatalogPort) {
        return new GetCardsService(loadCardCatalogPort);
    }

    @Bean
    public RecordSpendingUseCase recordSpendingUseCase(SaveSpendingRecordPort saveSpendingRecordPort) {
        return new RecordSpendingService(saveSpendingRecordPort);
    }

    @Bean
    public GetSpendingRecordsUseCase getSpendingRecordsUseCase(
            LoadSpendingRecordsByCardAndPeriodPort loadSpendingRecordsByCardAndPeriodPort
    ) {
        return new GetSpendingRecordsService(loadSpendingRecordsByCardAndPeriodPort);
    }

    @Bean
    public DeleteSpendingRecordUseCase deleteSpendingRecordUseCase(
            DeleteSpendingRecordPort deleteSpendingRecordPort
    ) {
        return new DeleteSpendingRecordService(deleteSpendingRecordPort);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    public RegisterCardUseCase registerCardUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            SaveCardPort saveCardPort,
            SaveCardPerformancePolicyPort saveCardPerformancePolicyPort
    ) {
        return new RegisterCardService(
                loadCardCatalogPort,
                saveCardPort,
                saveCardPerformancePolicyPort
        );
    }

    @Bean
    public UpdatePriorityUseCase updatePriorityUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            UpdateCardPriorityPort updateCardPriorityPort
    ) {
        return new UpdatePriorityService(loadCardCatalogPort, updateCardPriorityPort);
    }

    @Bean
    public GetCardPerformancePolicyUseCase getCardPerformancePolicyUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort
    ) {
        return new GetCardPerformancePolicyService(
                loadCardCatalogPort,
                loadCardPerformancePoliciesPort
        );
    }

    @Bean
    public ReplaceCardPerformancePolicyUseCase replaceCardPerformancePolicyUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            ReplaceCardPerformancePolicyPort replaceCardPerformancePolicyPort
    ) {
        return new ReplaceCardPerformancePolicyService(
                loadCardCatalogPort,
                replaceCardPerformancePolicyPort
        );
    }

    @Bean
    public GetPerformanceOverviewUseCase getPerformanceOverviewUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort,
            LoadSpendingRecordsPort loadSpendingRecordsPort
    ) {
        return new GetPerformanceOverviewService(
                loadCardCatalogPort,
                loadCardPerformancePoliciesPort,
                loadSpendingRecordsPort
        );
    }

    @Bean
    public SyncCardTransactionsUseCase syncCardTransactionsUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            FetchCardTransactionsPort fetchCardTransactionsPort,
            SaveSpendingRecordPort saveSpendingRecordPort
    ) {
        return new SyncCardTransactionsService(
                loadCardCatalogPort,
                fetchCardTransactionsPort,
                saveSpendingRecordPort
        );
    }

    @Bean
    public RecommendCardUseCase recommendCardUseCase(
            LoadCardCatalogPort loadCardCatalogPort,
            LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort,
            LoadSpendingRecordsPort loadSpendingRecordsPort
    ) {
        return new RecommendCardService(
                loadCardCatalogPort,
                loadCardPerformancePoliciesPort,
                loadSpendingRecordsPort
        );
    }

    @Bean
    public CommandLineRunner demoDataInitializer(
            LoadCardCatalogPort loadCardCatalogPort,
            SaveCardPort saveCardPort,
            SaveCardPerformancePolicyPort saveCardPerformancePolicyPort,
            SaveSpendingRecordPort saveSpendingRecordPort,
            RecommendationDemoScenarios recommendationDemoScenarios,
            TransactionNormalizer transactionNormalizer
    ) {
        return ignored -> {
            if (!loadCardCatalogPort.loadAll().isEmpty()) {
                return;
            }

            InMemoryCardCatalogAdapter seededCatalog = new InMemoryCardCatalogAdapter();
            for (var card : seededCatalog.loadAll()) {
                saveCardPort.save(card);
            }

            InMemoryCardPerformancePolicyAdapter seededPolicies = new InMemoryCardPerformancePolicyAdapter();
            for (var policy : seededPolicies.loadAll()) {
                saveCardPerformancePolicyPort.save(policy);
            }

            for (var spendingRecord : seedScenarioRecords(recommendationDemoScenarios, transactionNormalizer)) {
                saveSpendingRecordPort.save(spendingRecord);
            }
        };
    }

    private List<SpendingRecord> seedScenarioRecords(
            RecommendationDemoScenarios recommendationDemoScenarios,
            TransactionNormalizer transactionNormalizer
    ) {
        return recommendationDemoScenarios.scenarios().stream()
                .flatMap(scenario -> scenario.seedRecords().stream())
                .map(fixture -> {
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
                })
                .toList();
    }
}
