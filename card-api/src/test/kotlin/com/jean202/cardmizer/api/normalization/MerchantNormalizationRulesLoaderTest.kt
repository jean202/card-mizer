package com.jean202.cardmizer.api.normalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MerchantNormalizationRulesLoaderTest {
    @Test
    fun loadsDefaultYamlResource() {
        val rules = MerchantNormalizationRulesLoader().loadDefault()

        assertEquals("COFFEE", rules.categoryAliases["카페"])
        assertEquals("RESTAURANT", rules.categoryAliases["음식점"])
        assertTrue("KB_PAY" in rules.simplePayTags)
        assertTrue(rules.merchantRules.any { it.category == "MOVIE" })
        assertTrue(rules.merchantRules.any { it.category == "RESTAURANT" })
    }
}
