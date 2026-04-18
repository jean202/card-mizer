package com.jean202.cardmizer.api.normalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionNormalizerTest {
    private val transactionNormalizer = TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault())

    @Test
    fun infersCategoryAndOfflineTagFromMerchantName() {
        val normalized = transactionNormalizer.normalize("CGV 왕십리", null, emptySet())

        assertEquals("MOVIE", normalized.merchantCategory)
        assertTrue("OFFLINE" in normalized.paymentTags)
    }

    @Test
    fun canonicalizesExplicitCategoryAndCombinesSimplePayTags() {
        val normalized = transactionNormalizer.normalize(
            "스타벅스 강남",
            "카페",
            setOf("KB Pay", "온라인"),
        )

        assertEquals("COFFEE", normalized.merchantCategory)
        assertTrue("KB_PAY" in normalized.paymentTags)
        assertTrue("ONLINE" in normalized.paymentTags)
        assertTrue("SIMPLE_PAY" in normalized.paymentTags)
        assertTrue("SIMPLE_PAY_ONLINE" in normalized.paymentTags)
    }

    @Test
    fun infersSubscriptionTagsForOttMerchant() {
        val normalized = transactionNormalizer.normalize("넷플릭스", null, emptySet())

        assertEquals("OTT", normalized.merchantCategory)
        assertTrue("SUBSCRIPTION" in normalized.paymentTags)
        assertTrue("DIGITAL_CONTENT" in normalized.paymentTags)
        assertTrue("ONLINE" in normalized.paymentTags)
    }

    @Test
    fun infersCultureCategoryForTicketMerchant() {
        val normalized = transactionNormalizer.normalize("인터파크 티켓", null, emptySet())

        assertEquals("CULTURE", normalized.merchantCategory)
        assertTrue("ONLINE" in normalized.paymentTags)
    }

    @Test
    fun infersRestaurantCategoryForDiningMerchant() {
        val normalized = transactionNormalizer.normalize("맥도날드 선릉점", null, emptySet())

        assertEquals("RESTAURANT", normalized.merchantCategory)
        assertTrue("OFFLINE" in normalized.paymentTags)
    }

    @Test
    fun infersAutoBillTagForMobileCarrierMerchant() {
        val normalized = transactionNormalizer.normalize("SK텔레콤", null, emptySet())

        assertEquals("MOBILE_BILL", normalized.merchantCategory)
        assertTrue("AUTO_BILL" in normalized.paymentTags)
    }
}
