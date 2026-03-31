package com.jean202.cardmizer.api.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MerchantNormalizationRulesLoaderTest {
    @Test
    void loadsDefaultYamlResource() {
        MerchantNormalizationRules rules = new MerchantNormalizationRulesLoader().loadDefault();

        assertEquals("COFFEE", rules.categoryAliases().get("카페"));
        assertEquals("RESTAURANT", rules.categoryAliases().get("음식점"));
        assertTrue(rules.simplePayTags().contains("KB_PAY"));
        assertTrue(rules.merchantRules().stream().anyMatch(rule -> rule.category().equals("MOVIE")));
        assertTrue(rules.merchantRules().stream().anyMatch(rule -> rule.category().equals("RESTAURANT")));
    }
}
