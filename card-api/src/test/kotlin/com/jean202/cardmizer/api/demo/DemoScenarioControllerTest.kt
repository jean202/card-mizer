package com.jean202.cardmizer.api.demo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoScenarioControllerTest {
    @Test
    fun exposesScenarioTemplates() {
        val scenarios = RecommendationDemoScenarios(
            listOf(
                RecommendationDemoScenario(
                    id = "demo-1",
                    title = "Demo Title",
                    description = "Demo Description",
                    expectedRecommendedCardId = "SAMSUNG_KPASS",
                    request = DemoRecommendationRequest("2026-03", 20_000, "서울교통공사", null, emptySet()),
                    seedRecords = emptyList(),
                ),
            ),
        )
        val controller = DemoScenarioController(scenarios)

        val responses = controller.list()

        assertEquals(1, responses.size)
        assertEquals("demo-1", responses[0].id)
        assertEquals("2026-03", responses[0].request.spendingMonth)
        assertTrue(responses[0].expectedRecommendedCardId == "SAMSUNG_KPASS")
    }
}
