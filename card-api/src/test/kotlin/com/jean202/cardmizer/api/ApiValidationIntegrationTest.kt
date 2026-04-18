package com.jean202.cardmizer.api

import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ApiValidationIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun returnsStructuredFieldErrorsForInvalidRecommendationRequest() {
        mockMvc.perform(
            post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "spendingMonth": "202603",
                      "amount": 0,
                      "merchantName": " "
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/api/recommendations"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("spendingMonth", "amount", "merchantName")))
    }

    @Test
    fun validatesNestedCardPolicyRequest() {
        mockMvc.perform(
            put("/api/cards/SAMSUNG_KPASS/performance-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "tiers": [
                        {
                          "code": "",
                          "targetAmount": -1,
                          "benefitSummary": ""
                        }
                      ],
                      "benefitRules": [
                        {
                          "ruleId": "",
                          "benefitSummary": "",
                          "benefitType": "UNKNOWN"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/api/cards/SAMSUNG_KPASS/performance-policy"))
            .andExpect(
                jsonPath(
                    "$.fieldErrors[*].field",
                    hasItems(
                        "tiers[0].code",
                        "tiers[0].targetAmount",
                        "tiers[0].benefitSummary",
                        "benefitRules[0].ruleId",
                        "benefitRules[0].benefitSummary",
                        "benefitRules[0].benefitType",
                    ),
                ),
            )
    }

    @Test
    fun rejectsEmptyCardPolicyPatchRequest() {
        mockMvc.perform(
            patch("/api/cards/SAMSUNG_KPASS/performance-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/api/cards/SAMSUNG_KPASS/performance-policy"))
            .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("tiers", "benefitRules")))
    }

    @Test
    fun validatesConditionalBenefitRuleFieldsAsStructuredErrors() {
        mockMvc.perform(
            put("/api/cards/SAMSUNG_KPASS/performance-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "tiers": [
                        {
                          "code": "KPASS_40",
                          "targetAmount": 400000,
                          "benefitSummary": "전월 40만원 이상 혜택 구간"
                        }
                      ],
                      "benefitRules": [
                        {
                          "ruleId": "KPASS_TRANSIT",
                          "benefitSummary": "대중교통 할인",
                          "benefitType": "RATE_PERCENT",
                          "rateBasisPoints": 0,
                          "fixedBenefitAmount": 1000
                        },
                        {
                          "ruleId": "KPASS_CAFE",
                          "benefitSummary": "카페 할인",
                          "benefitType": "FIXED_AMOUNT",
                          "rateBasisPoints": 1000,
                          "fixedBenefitAmount": 0
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/api/cards/SAMSUNG_KPASS/performance-policy"))
            .andExpect(
                jsonPath(
                    "$.fieldErrors[*].field",
                    hasItems(
                        "benefitRules[0].rateBasisPoints",
                        "benefitRules[0].fixedBenefitAmount",
                        "benefitRules[1].rateBasisPoints",
                        "benefitRules[1].fixedBenefitAmount",
                    ),
                ),
            )
    }
}
