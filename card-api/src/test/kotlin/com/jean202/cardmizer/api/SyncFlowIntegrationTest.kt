package com.jean202.cardmizer.api

import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=18080",
        "cardcompany.api.base-url=http://localhost:18080/simulator/api/v1",
    ],
)
@AutoConfigureMockMvc
class SyncFlowIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun syncsTransactionsFromSimulatorAndReflectsInOverview() {
        mockMvc.perform(
            post("/api/sync/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"yearMonth":"2026-04"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fetchedCount", greaterThanOrEqualTo(40)))
            .andExpect(jsonPath("$.savedCount", greaterThanOrEqualTo(40)))
            .andExpect(jsonPath("$.syncedCardIds", hasItem("SAMSUNG_KPASS")))
            .andExpect(jsonPath("$.syncedCardIds", hasItem("KB_NORI2_KBPAY")))
            .andExpect(jsonPath("$.syncedCardIds", hasItem("KB_MY_WESH")))
            .andExpect(jsonPath("$.syncedCardIds", hasItem("HYUNDAI_ZERO_POINT")))

        mockMvc.perform(get("/api/performance-overview").queryParam("yearMonth", "2026-04"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.cardId == 'HYUNDAI_ZERO_POINT')].spentAmount", hasItem(greaterThanOrEqualTo(1))))
    }

    @Test
    fun simulatorRejectsUnauthenticatedDirectAccess() {
        mockMvc.perform(
            get("/simulator/api/v1/cards/SAMSUNG_KPASS/transactions").queryParam("yearMonth", "2026-03"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun simulatorRejectsInvalidApiKey() {
        mockMvc.perform(
            get("/simulator/api/v1/cards/SAMSUNG_KPASS/transactions")
                .queryParam("yearMonth", "2026-03")
                .header("X-Api-Key", "invalid-key"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun simulatorReturnsTransactionsWithValidApiKey() {
        mockMvc.perform(
            get("/simulator/api/v1/cards/KB_NORI2_KBPAY/transactions")
                .queryParam("yearMonth", "2026-03")
                .header("X-Api-Key", "sim-api-key-2026"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(greaterThanOrEqualTo(15))))
            .andExpect(jsonPath("$[0].txnId").isNotEmpty)
            .andExpect(jsonPath("$[0].cardNumber").value("KB_NORI2_KBPAY"))
            .andExpect(jsonPath("$[0].transactionDate").isNotEmpty)
            .andExpect(jsonPath("$[0].merchantName").isNotEmpty)
    }

    @Test
    fun rejectsInvalidYearMonthFormat() {
        mockMvc.perform(
            post("/api/sync/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"yearMonth":"202603"}"""),
        )
            .andExpect(status().isBadRequest)
    }
}
