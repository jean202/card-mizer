package com.jean202.cardmizer.api.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TransactionNormalizerTest {
    private final TransactionNormalizer transactionNormalizer = new TransactionNormalizer();

    @Test
    void infersCategoryAndOfflineTagFromMerchantName() {
        NormalizedTransaction normalized = transactionNormalizer.normalize("CGV 왕십리", null, Set.of());

        assertEquals("MOVIE", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("OFFLINE"));
    }

    @Test
    void canonicalizesExplicitCategoryAndCombinesSimplePayTags() {
        NormalizedTransaction normalized = transactionNormalizer.normalize(
                "스타벅스 강남",
                "카페",
                Set.of("KB Pay", "온라인")
        );

        assertEquals("COFFEE", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("KB_PAY"));
        assertTrue(normalized.paymentTags().contains("ONLINE"));
        assertTrue(normalized.paymentTags().contains("SIMPLE_PAY"));
        assertTrue(normalized.paymentTags().contains("SIMPLE_PAY_ONLINE"));
    }

    @Test
    void infersSubscriptionTagsForOttMerchant() {
        NormalizedTransaction normalized = transactionNormalizer.normalize("넷플릭스", null, Set.of());

        assertEquals("OTT", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("SUBSCRIPTION"));
        assertTrue(normalized.paymentTags().contains("DIGITAL_CONTENT"));
        assertTrue(normalized.paymentTags().contains("ONLINE"));
    }

    @Test
    void infersCultureCategoryForTicketMerchant() {
        NormalizedTransaction normalized = transactionNormalizer.normalize("인터파크 티켓", null, Set.of());

        assertEquals("CULTURE", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("ONLINE"));
    }

    @Test
    void infersRestaurantCategoryForDiningMerchant() {
        NormalizedTransaction normalized = transactionNormalizer.normalize("맥도날드 선릉점", null, Set.of());

        assertEquals("RESTAURANT", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("OFFLINE"));
    }

    @Test
    void infersAutoBillTagForMobileCarrierMerchant() {
        NormalizedTransaction normalized = transactionNormalizer.normalize("SK텔레콤", null, Set.of());

        assertEquals("MOBILE_BILL", normalized.merchantCategory());
        assertTrue(normalized.paymentTags().contains("AUTO_BILL"));
    }
}
