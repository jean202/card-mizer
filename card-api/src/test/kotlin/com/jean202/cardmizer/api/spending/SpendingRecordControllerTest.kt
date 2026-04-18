package com.jean202.cardmizer.api.spending

import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SpendingRecordControllerTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val normalizer = TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault())

    @Test
    fun normalizesSpendingRecordRequestBeforeSaving() {
        val useCase = CapturingRecordSpendingUseCase()
        val controller = SpendingRecordController(useCase, { _, _ -> emptyList() }, { }, normalizer, fixedClock)

        controller.create(
            SpendingRecordController.CreateSpendingRecordRequest(
                cardId = "SAMSUNG_KPASS",
                amount = 9_900,
                spentOn = LocalDate.of(2026, 3, 30),
                merchantName = "넷플릭스",
                merchantCategory = null,
                paymentTags = emptySet(),
            ),
        )

        assertEquals("OTT", useCase.saved!!.merchantCategory)
        assertTrue("SUBSCRIPTION" in useCase.saved!!.paymentTags)
        assertTrue("ONLINE" in useCase.saved!!.paymentTags)
    }

    private class CapturingRecordSpendingUseCase : RecordSpendingUseCase {
        var saved: SpendingRecord? = null
        override fun record(spendingRecord: SpendingRecord) { saved = spendingRecord }
    }
}
