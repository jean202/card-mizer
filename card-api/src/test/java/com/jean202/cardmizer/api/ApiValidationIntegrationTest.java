package com.jean202.cardmizer.api;

import static org.hamcrest.Matchers.hasItems;
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
class ApiValidationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStructuredFieldErrorsForInvalidRecommendationRequest() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spendingMonth": "202603",
                                  "amount": 0,
                                  "merchantName": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/recommendations"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("spendingMonth", "amount", "merchantName")));
    }

    @Test
    void validatesNestedCardPolicyRequest() throws Exception {
        mockMvc.perform(put("/api/cards/SAMSUNG_KPASS/performance-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/cards/SAMSUNG_KPASS/performance-policy"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems(
                        "tiers[0].code",
                        "tiers[0].targetAmount",
                        "tiers[0].benefitSummary",
                        "benefitRules[0].ruleId",
                        "benefitRules[0].benefitSummary",
                        "benefitRules[0].benefitType"
                )));
    }

    @Test
    void rejectsEmptyCardPolicyPatchRequest() throws Exception {
        mockMvc.perform(patch("/api/cards/SAMSUNG_KPASS/performance-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("At least one of tiers or benefitRules must be provided"))
                .andExpect(jsonPath("$.path").value("/api/cards/SAMSUNG_KPASS/performance-policy"));
    }
}
