package com.jean202.cardmizer.api.demo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecommendationDemoScenariosLoaderTest {
    @Test
    fun loadsDefaultRecommendationDemoScenarios() {
        val scenarios = RecommendationDemoScenariosLoader().loadDefault()

        assertEquals(4, scenarios.scenarios.size)
        assertTrue(scenarios.scenarios.any { it.id == "kpass-transit-threshold" })
        assertTrue(scenarios.scenarios.all { it.seedRecords.isNotEmpty() })
    }
}
