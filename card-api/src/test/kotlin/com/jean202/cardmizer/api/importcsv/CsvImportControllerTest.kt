package com.jean202.cardmizer.api.importcsv

import com.jean202.cardmizer.api.normalization.MerchantNormalizationRulesLoader
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.nio.charset.StandardCharsets

class CsvImportControllerTest {
    private val normalizer = TransactionNormalizer(MerchantNormalizationRulesLoader().loadDefault())

    @Test
    fun importsCsvRowsUsingRecordSpendingUseCase() {
        val useCase = CapturingRecordSpendingUseCase()
        val controller = CsvImportController(useCase, normalizer)

        val csv = """
            date,cardId,amount,merchantName,merchantCategory,paymentTags
            2026-03-05,KB_NORI2_KBPAY,5500,스타벅스 강남점,카페,KB_PAY
            2026-03-09,KB_NORI2_KBPAY,14000,CGV 왕십리,영화,KB_PAY|ONLINE
        """.trimIndent()
        val response = controller.importCsv(csvFile(csv))

        assertAll(
            { assertEquals(2, response.importedCount) },
            { assertEquals(0, response.errorCount) },
            { assertEquals(2, useCase.saved.size) },
            { assertEquals("스타벅스 강남점", useCase.saved[0].merchantName) },
            { assertEquals("CGV 왕십리", useCase.saved[1].merchantName) },
            { assertTrue("KB_PAY" in useCase.saved[1].paymentTags) },
            { assertTrue("ONLINE" in useCase.saved[1].paymentTags) },
        )
    }

    @Test
    fun normalizesTransactionsDuringImport() {
        val useCase = CapturingRecordSpendingUseCase()
        val controller = CsvImportController(useCase, normalizer)

        val csv = """
            date,cardId,amount,merchantName,merchantCategory,paymentTags
            2026-03-01,KB_MY_WESH,17000,넷플릭스,,
        """.trimIndent()
        controller.importCsv(csvFile(csv))

        val record = useCase.saved[0]
        assertAll(
            { assertEquals("OTT", record.merchantCategory) },
            { assertTrue("SUBSCRIPTION" in record.paymentTags) },
            { assertTrue("ONLINE" in record.paymentTags) },
        )
    }

    @Test
    fun collectsErrorsWithoutStoppingImport() {
        val useCase = CapturingRecordSpendingUseCase()
        val controller = CsvImportController(useCase, normalizer)

        val csv = """
            date,cardId,amount,merchantName,merchantCategory,paymentTags
            2026-03-05,KB_NORI2_KBPAY,5500,스타벅스 강남점,카페,KB_PAY
            bad-date,KB_NORI2_KBPAY,5500,이디야,,
            2026-03-10,KB_NORI2_KBPAY,3000,GS25 역삼점,편의점,
        """.trimIndent()
        val response = controller.importCsv(csvFile(csv))

        assertAll(
            { assertEquals(2, response.importedCount) },
            { assertEquals(1, response.errorCount) },
            { assertTrue(response.errors[0].contains("line 3")) },
        )
    }

    @Test
    fun rejectsEmptyFile() {
        val controller = CsvImportController({ }, normalizer)
        val emptyFile = MockMultipartFile("file", "empty.csv", "text/csv", ByteArray(0))

        assertThrows(IllegalArgumentException::class.java) { controller.importCsv(emptyFile) }
    }

    @Test
    fun skipsBlankLines() {
        val useCase = CapturingRecordSpendingUseCase()
        val controller = CsvImportController(useCase, normalizer)

        val csv = "date,cardId,amount,merchantName,merchantCategory,paymentTags\n" +
            "2026-03-05,SAMSUNG_KPASS,1250,서울교통공사,대중교통,\n" +
            "\n" +
            "2026-03-07,SAMSUNG_KPASS,1200,경기도버스,대중교통,\n"
        val response = controller.importCsv(csvFile(csv))

        assertEquals(2, response.importedCount)
    }

    private fun csvFile(content: String) =
        MockMultipartFile("file", "transactions.csv", "text/csv", content.toByteArray(StandardCharsets.UTF_8))

    private class CapturingRecordSpendingUseCase : RecordSpendingUseCase {
        val saved = mutableListOf<SpendingRecord>()
        override fun record(spendingRecord: SpendingRecord) { saved.add(spendingRecord) }
    }
}
