package com.jean202.cardmizer.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.jean202.cardmizer.api.demo.DemoSpendingRecordFixture
import com.jean202.cardmizer.api.demo.RecommendationDemoScenarios
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import javax.sql.DataSource

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class RecommendationApiEndToEndIntegrationTest {
    companion object {
        private val databaseName = "recommendation-e2e-${UUID.randomUUID()}"

        @DynamicPropertySource
        @JvmStatic
        fun overrideDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                "jdbc:h2:mem:$databaseName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var recommendationDemoScenarios: RecommendationDemoScenarios

    @Autowired
    private lateinit var transactionNormalizer: TransactionNormalizer

    @Autowired
    private lateinit var saveSpendingRecordPort: SaveSpendingRecordPort

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun demoRecommendationScenariosMatchExpectedCardsThroughJpaBackedApi() {
        for (scenario in recommendationDemoScenarios.scenarios) {
            replaceScenarioRecords(scenario.seedRecords)
            val request = scenario.request
            val payload = mapOf(
                "spendingMonth" to request.spendingMonth,
                "amount" to request.amount,
                "merchantName" to request.merchantName,
                "merchantCategory" to request.merchantCategory,
                "paymentTags" to request.paymentTags,
            )

            mockMvc.perform(
                post("/api/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.recommendedCardId").value(scenario.expectedRecommendedCardId))
                .andExpect(jsonPath("$.recommendedCardName").isNotEmpty)
                .andExpect(jsonPath("$.reason").isNotEmpty)
        }
    }

    private fun replaceScenarioRecords(fixtures: List<DemoSpendingRecordFixture>) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM spending_records")
            }
        }
        fixtures.map { toDomain(it) }.forEach { saveSpendingRecordPort.save(it) }
    }

    private fun toDomain(fixture: DemoSpendingRecordFixture): SpendingRecord {
        val normalized = transactionNormalizer.normalize(
            fixture.merchantName,
            fixture.merchantCategory,
            fixture.paymentTags,
        )
        return SpendingRecord(
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
