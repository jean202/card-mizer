package com.jean202.cardmizer.api.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecommendationDemoScenariosLoaderTest {
    @Test
    void loadsDefaultRecommendationDemoScenarios() {
        RecommendationDemoScenarios scenarios = new RecommendationDemoScenariosLoader().loadDefault();

        assertEquals(4, scenarios.scenarios().size());
        assertTrue(scenarios.scenarios().stream().anyMatch(scenario -> scenario.id().equals("kpass-transit-threshold")));
        assertTrue(scenarios.scenarios().stream().allMatch(scenario -> !scenario.seedRecords().isEmpty()));
    }
}
