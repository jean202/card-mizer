package com.jean202.cardmizer.api.demo;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public class RecommendationDemoScenariosLoader {
    static final String DEFAULT_RESOURCE_PATH = "demo/recommendation-scenarios.yml";

    public RecommendationDemoScenarios loadDefault() {
        return load(DEFAULT_RESOURCE_PATH);
    }

    public RecommendationDemoScenarios load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Recommendation demo scenarios resource not found: " + resourcePath);
            }

            Object loaded = new Yaml().load(inputStream);
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalStateException("Recommendation demo scenarios must be a YAML object: " + resourcePath);
            }

            return new RecommendationDemoScenarios(scenarios(root.get("scenarios")));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load recommendation demo scenarios: " + resourcePath, exception);
        }
    }

    private static List<RecommendationDemoScenario> scenarios(Object value) {
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalStateException("Expected scenarios YAML list");
        }

        List<RecommendationDemoScenario> scenarios = new ArrayList<>();
        for (Object rawScenario : rawList) {
            if (!(rawScenario instanceof Map<?, ?> scenarioMap)) {
                throw new IllegalStateException("Scenario must be an object");
            }
            scenarios.add(new RecommendationDemoScenario(
                    stringValue(scenarioMap.get("id")),
                    stringValue(scenarioMap.get("title")),
                    stringValue(scenarioMap.get("description")),
                    stringValue(scenarioMap.get("expectedRecommendedCardId")),
                    request(scenarioMap.get("request")),
                    seedRecords(scenarioMap.get("seedRecords"))
            ));
        }
        return List.copyOf(scenarios);
    }

    private static DemoRecommendationRequest request(Object value) {
        if (!(value instanceof Map<?, ?> requestMap)) {
            throw new IllegalStateException("Scenario request must be an object");
        }
        return new DemoRecommendationRequest(
                stringValue(requestMap.get("spendingMonth")),
                longValue(requestMap.get("amount")),
                stringValue(requestMap.get("merchantName")),
                optionalStringValue(requestMap.get("merchantCategory")),
                stringSet(requestMap.get("paymentTags"))
        );
    }

    private static List<DemoSpendingRecordFixture> seedRecords(Object value) {
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalStateException("Scenario seedRecords must be a list");
        }

        List<DemoSpendingRecordFixture> fixtures = new ArrayList<>();
        for (Object rawRecord : rawList) {
            if (!(rawRecord instanceof Map<?, ?> recordMap)) {
                throw new IllegalStateException("Seed record must be an object");
            }
            fixtures.add(new DemoSpendingRecordFixture(
                    stringValue(recordMap.get("cardId")),
                    longValue(recordMap.get("amount")),
                    LocalDate.parse(stringValue(recordMap.get("spentOn"))),
                    stringValue(recordMap.get("merchantName")),
                    optionalStringValue(recordMap.get("merchantCategory")),
                    stringSet(recordMap.get("paymentTags"))
            ));
        }
        return List.copyOf(fixtures);
    }

    private static Set<String> stringSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalStateException("Expected YAML list but got: " + value);
        }
        Set<String> values = new LinkedHashSet<>();
        for (Object item : rawList) {
            values.add(stringValue(item));
        }
        return Set.copyOf(values);
    }

    private static String stringValue(Object value) {
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalStateException("Expected non-blank string but got: " + value);
        }
        return stringValue;
    }

    private static String optionalStringValue(Object value) {
        if (value == null) {
            return null;
        }
        return stringValue(value);
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Expected numeric value but got: " + value);
    }
}
