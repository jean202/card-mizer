package com.jean202.cardmizer.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JpaPersistenceFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void persistsCardPolicyAndSpendingAcrossHttpFlows() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardId": "JPA_TEST_CARD",
                                  "issuerName": "테스트카드",
                                  "productName": "Persistence",
                                  "cardType": "CREDIT",
                                  "priority": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").value("JPA_TEST_CARD"));

        mockMvc.perform(get("/api/cards/JPA_TEST_CARD/performance-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value("JPA_TEST_CARD"))
                .andExpect(jsonPath("$.tiers[0].code").value("DEFAULT_BASE"));

        mockMvc.perform(put("/api/cards/JPA_TEST_CARD/performance-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tiers": [
                                    {
                                      "code": "TEST_30",
                                      "targetAmount": 300000,
                                      "benefitSummary": "전월 30만원 이상 혜택 구간"
                                    }
                                  ],
                                  "benefitRules": [
                                    {
                                      "ruleId": "TEST_OTT",
                                      "benefitSummary": "OTT 10% 할인",
                                      "benefitType": "RATE_PERCENT",
                                      "merchantCategories": ["OTT"],
                                      "requiredTags": ["SUBSCRIPTION"],
                                      "rateBasisPoints": 1000,
                                      "minimumPreviousMonthSpent": 300000,
                                      "monthlyCapTiers": [
                                        {
                                          "minimumPreviousMonthSpent": 0,
                                          "monthlyCap": 10000
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiers[0].code").value("TEST_30"))
                .andExpect(jsonPath("$.benefitRules[0].ruleId").value("TEST_OTT"));

        mockMvc.perform(patch("/api/cards/JPA_TEST_CARD/performance-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benefitRules": [
                                    {
                                      "ruleId": "TEST_STREAMING",
                                      "benefitSummary": "스트리밍 15% 할인",
                                      "benefitType": "RATE_PERCENT",
                                      "merchantCategories": ["OTT"],
                                      "requiredTags": ["SUBSCRIPTION"],
                                      "rateBasisPoints": 1500,
                                      "minimumPreviousMonthSpent": 300000,
                                      "monthlyCapTiers": [
                                        {
                                          "minimumPreviousMonthSpent": 0,
                                          "monthlyCap": 15000
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiers[0].code").value("TEST_30"))
                .andExpect(jsonPath("$.benefitRules[0].ruleId").value("TEST_STREAMING"));

        mockMvc.perform(get("/api/cards/JPA_TEST_CARD/performance-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiers[0].code").value("TEST_30"))
                .andExpect(jsonPath("$.benefitRules[0].ruleId").value("TEST_STREAMING"));

        mockMvc.perform(post("/api/spending-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardId": "JPA_TEST_CARD",
                                  "amount": 10000,
                                  "spentOn": "2026-03-31",
                                  "merchantName": "넷플릭스",
                                  "paymentTags": []
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/performance-overview")
                        .queryParam("yearMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cardId == 'JPA_TEST_CARD')].spentAmount", hasItem(10000)))
                .andExpect(jsonPath("$[?(@.cardId == 'JPA_TEST_CARD')].targetTierCode", hasItem("TEST_30")));

        mockMvc.perform(patch("/api/cards/priorities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedCardIds": [
                                    "JPA_TEST_CARD",
                                    "SAMSUNG_KPASS",
                                    "KB_MY_WESH",
                                    "KB_NORI2_KBPAY",
                                    "HYUNDAI_ZERO_POINT"
                                  ]
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/performance-overview")
                        .queryParam("yearMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardId").value("JPA_TEST_CARD"));
    }
}
