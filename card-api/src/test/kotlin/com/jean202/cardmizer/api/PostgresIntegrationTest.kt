package com.jean202.cardmizer.api

import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("cardmizer")
            .withUsername("cardmizer")
            .withPassword("cardmizer")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "true" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @Sql(scripts = ["/cleanup-postgres-test.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun flywayMigrationsAndFullFlowOnPostgres() {
        mockMvc.perform(
            post("/api/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"cardId":"PG_TEST_CARD","issuerName":"테스트카드","productName":"PostgreSQL 검증","cardType":"CREDIT","priority":5}""",
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.cardId").value("PG_TEST_CARD"))

        mockMvc.perform(get("/api/cards/PG_TEST_CARD/performance-policy"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cardId").value("PG_TEST_CARD"))
            .andExpect(jsonPath("$.tiers[0].code").value("DEFAULT_BASE"))

        mockMvc.perform(
            put("/api/cards/PG_TEST_CARD/performance-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"tiers":[{"code":"PG_30","targetAmount":300000,"benefitSummary":"30만원 구간"},
                    {"code":"PG_60","targetAmount":600000,"benefitSummary":"60만원 구간"}],
                    "benefitRules":[
                    {"ruleId":"PG_OTT","benefitSummary":"OTT 10%% 할인","benefitType":"RATE_PERCENT",
                    "merchantCategories":["OTT"],"requiredTags":["SUBSCRIPTION"],"rateBasisPoints":1000,
                    "minimumPreviousMonthSpent":300000,"monthlyCapTiers":[{"minimumPreviousMonthSpent":0,"monthlyCap":5000},{"minimumPreviousMonthSpent":300000,"monthlyCap":10000}]},
                    {"ruleId":"PG_CAFE","benefitSummary":"카페 300원 할인","benefitType":"FIXED_AMOUNT",
                    "merchantCategories":["COFFEE"],"fixedBenefitAmount":300,"monthlyCountLimit":10}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tiers[0].code").value("PG_30"))
            .andExpect(jsonPath("$.tiers[1].code").value("PG_60"))
            .andExpect(jsonPath("$.benefitRules[0].ruleId").value("PG_OTT"))
            .andExpect(jsonPath("$.benefitRules[0].monthlyCapTiers[1].monthlyCap").value(10000))
            .andExpect(jsonPath("$.benefitRules[1].ruleId").value("PG_CAFE"))
            .andExpect(jsonPath("$.benefitRules[1].fixedBenefitAmount").value(300))

        mockMvc.perform(
            patch("/api/cards/PG_TEST_CARD/performance-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"tiers":[{"code":"PG_50","targetAmount":500000,"benefitSummary":"50만원 단일 구간"}]}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tiers[0].code").value("PG_50"))
            .andExpect(jsonPath("$.benefitRules[0].ruleId").value("PG_OTT"))

        mockMvc.perform(
            post("/api/spending-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"cardId":"PG_TEST_CARD","amount":15000,"spentOn":"2026-03-15","merchantName":"넷플릭스","paymentTags":[]}""",
                ),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(get("/api/performance-overview").queryParam("yearMonth", "2026-03"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.cardId == 'PG_TEST_CARD')].spentAmount", hasItem(15000)))

        mockMvc.perform(get("/api/cards"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.cardId == 'PG_TEST_CARD')].issuerName", hasItem("테스트카드")))

        mockMvc.perform(
            post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"spendingMonth":"2026-03","amount":10000,"merchantName":"스타벅스","paymentTags":[]}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.recommendedCardId").isNotEmpty)

        mockMvc.perform(
            patch("/api/cards/priorities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"orderedCardIds":["PG_TEST_CARD","SAMSUNG_KPASS","KB_MY_WESH","KB_NORI2_KBPAY","HYUNDAI_ZERO_POINT"]}""",
                ),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/performance-overview").queryParam("yearMonth", "2026-03"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].cardId").value("PG_TEST_CARD"))
    }
}
