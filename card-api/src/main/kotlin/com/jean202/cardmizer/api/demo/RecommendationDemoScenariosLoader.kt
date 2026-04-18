package com.jean202.cardmizer.api.demo

import org.yaml.snakeyaml.Yaml
import java.time.LocalDate

class RecommendationDemoScenariosLoader {

    fun loadDefault(): RecommendationDemoScenarios = load(DEFAULT_RESOURCE_PATH)

    fun load(resourcePath: String): RecommendationDemoScenarios {
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Recommendation demo scenarios resource not found: $resourcePath")

        inputStream.use { stream ->
            val loaded = Yaml().load<Any>(stream)
            val root = loaded as? Map<*, *>
                ?: throw IllegalStateException("Recommendation demo scenarios must be a YAML object: $resourcePath")

            return RecommendationDemoScenarios(scenarios(root["scenarios"]))
        }
    }

    companion object {
        const val DEFAULT_RESOURCE_PATH = "demo/recommendation-scenarios.yml"

        private fun scenarios(value: Any?): List<RecommendationDemoScenario> {
            val rawList = value as? List<*>
                ?: throw IllegalStateException("Expected scenarios YAML list")
            return rawList.map { rawScenario ->
                val scenarioMap = rawScenario as? Map<*, *>
                    ?: throw IllegalStateException("Scenario must be an object")
                RecommendationDemoScenario(
                    id = stringValue(scenarioMap["id"]),
                    title = stringValue(scenarioMap["title"]),
                    description = stringValue(scenarioMap["description"]),
                    expectedRecommendedCardId = stringValue(scenarioMap["expectedRecommendedCardId"]),
                    request = request(scenarioMap["request"]),
                    seedRecords = seedRecords(scenarioMap["seedRecords"]),
                )
            }
        }

        private fun request(value: Any?): DemoRecommendationRequest {
            val requestMap = value as? Map<*, *>
                ?: throw IllegalStateException("Scenario request must be an object")
            return DemoRecommendationRequest(
                spendingMonth = stringValue(requestMap["spendingMonth"]),
                amount = longValue(requestMap["amount"]),
                merchantName = stringValue(requestMap["merchantName"]),
                merchantCategory = optionalStringValue(requestMap["merchantCategory"]),
                paymentTags = stringSet(requestMap["paymentTags"]),
            )
        }

        private fun seedRecords(value: Any?): List<DemoSpendingRecordFixture> {
            val rawList = value as? List<*>
                ?: throw IllegalStateException("Scenario seedRecords must be a list")
            return rawList.map { rawRecord ->
                val recordMap = rawRecord as? Map<*, *>
                    ?: throw IllegalStateException("Seed record must be an object")
                DemoSpendingRecordFixture(
                    cardId = stringValue(recordMap["cardId"]),
                    amount = longValue(recordMap["amount"]),
                    spentOn = LocalDate.parse(stringValue(recordMap["spentOn"])),
                    merchantName = stringValue(recordMap["merchantName"]),
                    merchantCategory = optionalStringValue(recordMap["merchantCategory"]),
                    paymentTags = stringSet(recordMap["paymentTags"]),
                )
            }
        }

        private fun stringSet(value: Any?): Set<String> {
            value ?: return emptySet()
            val rawList = value as? List<*>
                ?: throw IllegalStateException("Expected YAML list but got: $value")
            return rawList.map { stringValue(it) }.toSet()
        }

        private fun stringValue(value: Any?): String {
            val s = value as? String
            require(!s.isNullOrBlank()) { "Expected non-blank string but got: $value" }
            return s
        }

        private fun optionalStringValue(value: Any?): String? =
            if (value == null) null else stringValue(value)

        private fun longValue(value: Any?): Long =
            (value as? Number)?.toLong()
                ?: throw IllegalStateException("Expected numeric value but got: $value")
    }
}
