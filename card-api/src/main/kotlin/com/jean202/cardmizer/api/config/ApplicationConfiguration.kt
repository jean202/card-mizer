package com.jean202.cardmizer.api.config

import com.jean202.cardmizer.api.demo.RecommendationDemoScenarios
import com.jean202.cardmizer.api.demo.RecommendationDemoScenariosLoader
import com.jean202.cardmizer.api.normalization.MerchantNormalizationRules
import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.application.DeleteSpendingRecordService
import com.jean202.cardmizer.core.application.GetCardPerformancePolicyService
import com.jean202.cardmizer.core.application.GetCardsService
import com.jean202.cardmizer.core.application.GetPerformanceOverviewService
import com.jean202.cardmizer.core.application.GetSpendingRecordsService
import com.jean202.cardmizer.core.application.RecommendCardService
import com.jean202.cardmizer.core.application.RecordSpendingService
import com.jean202.cardmizer.core.application.RegisterCardService
import com.jean202.cardmizer.core.application.ReplaceCardPerformancePolicyService
import com.jean202.cardmizer.core.application.SyncCardTransactionsService
import com.jean202.cardmizer.core.application.UpdatePriorityService
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.DeleteSpendingRecordUseCase
import com.jean202.cardmizer.core.port.`in`.GetCardPerformancePolicyUseCase
import com.jean202.cardmizer.core.port.`in`.GetCardsUseCase
import com.jean202.cardmizer.core.port.`in`.GetPerformanceOverviewUseCase
import com.jean202.cardmizer.core.port.`in`.GetSpendingRecordsUseCase
import com.jean202.cardmizer.core.port.`in`.RecommendCardUseCase
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import com.jean202.cardmizer.core.port.`in`.RegisterCardUseCase
import com.jean202.cardmizer.core.port.`in`.ReplaceCardPerformancePolicyUseCase
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase
import com.jean202.cardmizer.core.port.`in`.UpdatePriorityUseCase
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort
import com.jean202.cardmizer.core.port.out.SaveCardPort
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import com.jean202.cardmizer.core.port.out.UpdateCardPriorityPort
import com.jean202.cardmizer.infra.persistence.InMemoryCardCatalogAdapter
import com.jean202.cardmizer.infra.persistence.InMemoryCardPerformancePolicyAdapter
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId
import java.util.UUID

@Configuration
class ApplicationConfiguration {

    @Bean
    fun merchantNormalizationRules(): MerchantNormalizationRules =
        MerchantNormalizationRulesLoader().loadDefault()

    @Bean
    fun transactionNormalizer(merchantNormalizationRules: MerchantNormalizationRules): TransactionNormalizer =
        TransactionNormalizer(merchantNormalizationRules)

    @Bean
    fun recommendationDemoScenarios(): RecommendationDemoScenarios =
        RecommendationDemoScenariosLoader().loadDefault()

    @Bean
    fun getCardsUseCase(loadCardCatalogPort: LoadCardCatalogPort): GetCardsUseCase =
        GetCardsService(loadCardCatalogPort)

    @Bean
    fun recordSpendingUseCase(saveSpendingRecordPort: SaveSpendingRecordPort): RecordSpendingUseCase =
        RecordSpendingService(saveSpendingRecordPort)

    @Bean
    fun getSpendingRecordsUseCase(
        loadSpendingRecordsByCardAndPeriodPort: LoadSpendingRecordsByCardAndPeriodPort,
    ): GetSpendingRecordsUseCase = GetSpendingRecordsService(loadSpendingRecordsByCardAndPeriodPort)

    @Bean
    fun deleteSpendingRecordUseCase(deleteSpendingRecordPort: DeleteSpendingRecordPort): DeleteSpendingRecordUseCase =
        DeleteSpendingRecordService(deleteSpendingRecordPort)

    @Bean
    fun clock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))

    @Bean
    fun registerCardUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        saveCardPort: SaveCardPort,
        saveCardPerformancePolicyPort: SaveCardPerformancePolicyPort,
    ): RegisterCardUseCase = RegisterCardService(loadCardCatalogPort, saveCardPort, saveCardPerformancePolicyPort)

    @Bean
    fun updatePriorityUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        updateCardPriorityPort: UpdateCardPriorityPort,
    ): UpdatePriorityUseCase = UpdatePriorityService(loadCardCatalogPort, updateCardPriorityPort)

    @Bean
    fun getCardPerformancePolicyUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
    ): GetCardPerformancePolicyUseCase = GetCardPerformancePolicyService(loadCardCatalogPort, loadCardPerformancePoliciesPort)

    @Bean
    fun replaceCardPerformancePolicyUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        replaceCardPerformancePolicyPort: ReplaceCardPerformancePolicyPort,
    ): ReplaceCardPerformancePolicyUseCase = ReplaceCardPerformancePolicyService(loadCardCatalogPort, replaceCardPerformancePolicyPort)

    @Bean
    fun getPerformanceOverviewUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
        loadSpendingRecordsPort: LoadSpendingRecordsPort,
    ): GetPerformanceOverviewUseCase = GetPerformanceOverviewService(
        loadCardCatalogPort, loadCardPerformancePoliciesPort, loadSpendingRecordsPort,
    )

    @Bean
    fun syncCardTransactionsUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        fetchCardTransactionsPort: FetchCardTransactionsPort,
        saveSpendingRecordPort: SaveSpendingRecordPort,
    ): SyncCardTransactionsUseCase = SyncCardTransactionsService(
        loadCardCatalogPort, fetchCardTransactionsPort, saveSpendingRecordPort,
    )

    @Bean
    fun recommendCardUseCase(
        loadCardCatalogPort: LoadCardCatalogPort,
        loadCardPerformancePoliciesPort: LoadCardPerformancePoliciesPort,
        loadSpendingRecordsPort: LoadSpendingRecordsPort,
    ): RecommendCardUseCase = RecommendCardService(
        loadCardCatalogPort, loadCardPerformancePoliciesPort, loadSpendingRecordsPort,
    )

    @Bean
    fun demoDataInitializer(
        loadCardCatalogPort: LoadCardCatalogPort,
        saveCardPort: SaveCardPort,
        saveCardPerformancePolicyPort: SaveCardPerformancePolicyPort,
        saveSpendingRecordPort: SaveSpendingRecordPort,
        recommendationDemoScenarios: RecommendationDemoScenarios,
        transactionNormalizer: TransactionNormalizer,
    ): CommandLineRunner = CommandLineRunner {
        if (loadCardCatalogPort.loadAll().isNotEmpty()) return@CommandLineRunner

        InMemoryCardCatalogAdapter().loadAll().forEach { saveCardPort.save(it) }
        InMemoryCardPerformancePolicyAdapter().loadAll().forEach { saveCardPerformancePolicyPort.save(it) }
        seedScenarioRecords(recommendationDemoScenarios, transactionNormalizer)
            .forEach { saveSpendingRecordPort.save(it) }
    }

    private fun seedScenarioRecords(
        recommendationDemoScenarios: RecommendationDemoScenarios,
        transactionNormalizer: TransactionNormalizer,
    ): List<SpendingRecord> =
        recommendationDemoScenarios.scenarios.flatMap { it.seedRecords }.map { fixture ->
            val normalized = transactionNormalizer.normalize(
                fixture.merchantName, fixture.merchantCategory, fixture.paymentTags,
            )
            SpendingRecord(
                id = UUID.randomUUID(),
                cardId = CardId(fixture.cardId),
                amount = Money.won(fixture.amount),
                spentOn = fixture.spentOn,
                merchantName = fixture.merchantName,
                merchantCategory = normalized.merchantCategory,
                paymentTags = normalized.paymentTags,
            )
        }
}
