package com.jean202.cardmizer.api.normalization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public class MerchantNormalizationRulesLoader {
    static final String DEFAULT_RESOURCE_PATH = "normalization/merchant-rules.yml";

    public MerchantNormalizationRules loadDefault() {
        return load(DEFAULT_RESOURCE_PATH);
    }

    public MerchantNormalizationRules load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Normalization rules resource not found: " + resourcePath);
            }

            Object loaded = new Yaml().load(inputStream);
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalStateException("Normalization rules must be a YAML object: " + resourcePath);
            }

            return new MerchantNormalizationRules(
                    stringMap(root.get("categoryAliases")),
                    stringMap(root.get("tagAliases")),
                    stringSet(root.get("simplePayTags")),
                    stringSet(root.get("onlineCategories")),
                    stringSet(root.get("offlineCategories")),
                    stringSet(root.get("subscriptionCategories")),
                    stringSet(root.get("digitalContentCategories")),
                    merchantRules(root.get("merchantRules"))
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load normalization rules: " + resourcePath, exception);
        }
    }

    private static Map<String, String> stringMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalStateException("Expected YAML map but got: " + value.getClass().getSimpleName());
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            result.put(asString(entry.getKey()), asString(entry.getValue()));
        }
        return Map.copyOf(result);
    }

    private static Set<String> stringSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalStateException("Expected YAML list but got: " + value.getClass().getSimpleName());
        }

        Set<String> result = new LinkedHashSet<>();
        for (Object item : rawList) {
            result.add(asString(item));
        }
        return Set.copyOf(result);
    }

    private static List<MerchantNormalizationRule> merchantRules(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalStateException("Expected YAML list but got: " + value.getClass().getSimpleName());
        }

        List<MerchantNormalizationRule> result = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawRule)) {
                throw new IllegalStateException("Expected merchant rule object but got: " + item);
            }
            result.add(new MerchantNormalizationRule(
                    asString(rawRule.get("category")),
                    stringSet(rawRule.get("keywords")),
                    stringSet(rawRule.get("inferredTags"))
            ));
        }
        return List.copyOf(result);
    }

    private static String asString(Object value) {
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalStateException("Expected non-blank string but got: " + value);
        }
        return stringValue;
    }
}
