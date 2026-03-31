package com.jean202.cardmizer.api.demo;

import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo-scenarios/recommendations")
public class DemoScenarioController {
    private final RecommendationDemoScenarios recommendationDemoScenarios;

    public DemoScenarioController(RecommendationDemoScenarios recommendationDemoScenarios) {
        this.recommendationDemoScenarios = recommendationDemoScenarios;
    }

    @GetMapping
    public List<RecommendationDemoScenarioResponse> list() {
        return recommendationDemoScenarios.scenarios().stream()
                .map(RecommendationDemoScenarioResponse::from)
                .toList();
    }

    public record RecommendationDemoScenarioResponse(
            String id,
            String title,
            String description,
            String expectedRecommendedCardId,
            int seedRecordCount,
            RecommendationRequestTemplate request
    ) {
        static RecommendationDemoScenarioResponse from(RecommendationDemoScenario scenario) {
            return new RecommendationDemoScenarioResponse(
                    scenario.id(),
                    scenario.title(),
                    scenario.description(),
                    scenario.expectedRecommendedCardId(),
                    scenario.seedRecords().size(),
                    RecommendationRequestTemplate.from(scenario.request())
            );
        }
    }

    public record RecommendationRequestTemplate(
            String spendingMonth,
            long amount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags
    ) {
        static RecommendationRequestTemplate from(DemoRecommendationRequest request) {
            return new RecommendationRequestTemplate(
                    request.spendingMonth(),
                    request.amount(),
                    request.merchantName(),
                    request.merchantCategory(),
                    request.paymentTags()
            );
        }
    }
}
