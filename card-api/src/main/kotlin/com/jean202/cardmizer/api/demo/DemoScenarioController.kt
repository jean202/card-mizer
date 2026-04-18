package com.jean202.cardmizer.api.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/demo-scenarios/recommendations")
class DemoScenarioController(
    private val recommendationDemoScenarios: RecommendationDemoScenarios,
) {
    @GetMapping
    fun list(): List<RecommendationDemoScenarioResponse> =
        recommendationDemoScenarios.scenarios.map { RecommendationDemoScenarioResponse.from(it) }

    data class RecommendationDemoScenarioResponse(
        val id: String,
        val title: String,
        val description: String,
        val expectedRecommendedCardId: String,
        val seedRecordCount: Int,
        val request: RecommendationRequestTemplate,
    ) {
        companion object {
            fun from(scenario: RecommendationDemoScenario) = RecommendationDemoScenarioResponse(
                id = scenario.id,
                title = scenario.title,
                description = scenario.description,
                expectedRecommendedCardId = scenario.expectedRecommendedCardId,
                seedRecordCount = scenario.seedRecords.size,
                request = RecommendationRequestTemplate.from(scenario.request),
            )
        }
    }

    data class RecommendationRequestTemplate(
        val spendingMonth: String,
        val amount: Long,
        val merchantName: String,
        val merchantCategory: String?,
        val paymentTags: Set<String>,
    ) {
        companion object {
            fun from(request: DemoRecommendationRequest) = RecommendationRequestTemplate(
                spendingMonth = request.spendingMonth,
                amount = request.amount,
                merchantName = request.merchantName,
                merchantCategory = request.merchantCategory,
                paymentTags = request.paymentTags,
            )
        }
    }
}
