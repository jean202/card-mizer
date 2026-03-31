package com.jean202.cardmizer.api.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DemoScenarioControllerTest {
    @Test
    void exposesScenarioTemplates() {
        RecommendationDemoScenarios scenarios = new RecommendationDemoScenarios(List.of(
                new RecommendationDemoScenario(
                        "demo-1",
                        "Demo Title",
                        "Demo Description",
                        "SAMSUNG_KPASS",
                        new DemoRecommendationRequest("2026-03", 20_000, "서울교통공사", null, Set.of()),
                        List.of()
                )
        ));
        DemoScenarioController controller = new DemoScenarioController(scenarios);

        List<DemoScenarioController.RecommendationDemoScenarioResponse> responses = controller.list();

        assertEquals(1, responses.size());
        assertEquals("demo-1", responses.get(0).id());
        assertEquals("2026-03", responses.get(0).request().spendingMonth());
        assertTrue(responses.get(0).expectedRecommendedCardId().equals("SAMSUNG_KPASS"));
    }
}
