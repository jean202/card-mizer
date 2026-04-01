package com.jean202.cardmizer.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18080",
                "cardcompany.api.base-url=http://localhost:18080/simulator/api/v1"
        }
)
@AutoConfigureMockMvc
class SyncFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void syncsTransactionsFromSimulatorAndReflectsInOverview() throws Exception {
        mockMvc.perform(post("/api/sync/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "yearMonth": "2026-04"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetchedCount", greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.savedCount", greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.syncedCardIds", hasItem("SAMSUNG_KPASS")))
                .andExpect(jsonPath("$.syncedCardIds", hasItem("KB_NORI2_KBPAY")))
                .andExpect(jsonPath("$.syncedCardIds", hasItem("KB_MY_WESH")))
                .andExpect(jsonPath("$.syncedCardIds", hasItem("HYUNDAI_ZERO_POINT")));

        mockMvc.perform(get("/api/performance-overview")
                        .queryParam("yearMonth", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cardId == 'HYUNDAI_ZERO_POINT')].spentAmount",
                        hasItem(greaterThanOrEqualTo(1))));
    }

    @Test
    void simulatorRejectsUnauthenticatedDirectAccess() throws Exception {
        mockMvc.perform(get("/simulator/api/v1/cards/SAMSUNG_KPASS/transactions")
                        .queryParam("yearMonth", "2026-03"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void simulatorRejectsInvalidApiKey() throws Exception {
        mockMvc.perform(get("/simulator/api/v1/cards/SAMSUNG_KPASS/transactions")
                        .queryParam("yearMonth", "2026-03")
                        .header("X-Api-Key", "invalid-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void simulatorReturnsTransactionsWithValidApiKey() throws Exception {
        mockMvc.perform(get("/simulator/api/v1/cards/KB_NORI2_KBPAY/transactions")
                        .queryParam("yearMonth", "2026-03")
                        .header("X-Api-Key", "sim-api-key-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(15))))
                .andExpect(jsonPath("$[0].txnId").isNotEmpty())
                .andExpect(jsonPath("$[0].cardNumber").value("KB_NORI2_KBPAY"))
                .andExpect(jsonPath("$[0].transactionDate").isNotEmpty())
                .andExpect(jsonPath("$[0].merchantName").isNotEmpty());
    }

    @Test
    void rejectsInvalidYearMonthFormat() throws Exception {
        mockMvc.perform(post("/api/sync/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "yearMonth": "202603"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
