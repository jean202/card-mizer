package com.jean202.cardmizer.api.demo

class RecommendationDemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val expectedRecommendedCardId: String,
    val request: DemoRecommendationRequest,
    seedRecords: List<DemoSpendingRecordFixture>,
) {
    val seedRecords: List<DemoSpendingRecordFixture>

    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(description.isNotBlank()) { "description must not be blank" }
        require(expectedRecommendedCardId.isNotBlank()) { "expectedRecommendedCardId must not be blank" }
        this.seedRecords = seedRecords.toList()
    }
}
