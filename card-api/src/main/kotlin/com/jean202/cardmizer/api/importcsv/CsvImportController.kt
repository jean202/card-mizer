package com.jean202.cardmizer.api.importcsv

import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

@RestController
@RequestMapping("/api/import")
class CsvImportController(
    private val recordSpendingUseCase: RecordSpendingUseCase,
    private val transactionNormalizer: TransactionNormalizer,
) {
    @PostMapping(value = ["/spending-records"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCsv(@RequestParam("file") file: MultipartFile): CsvImportResponse {
        require(!file.isEmpty) { "Uploaded file is empty" }

        val errors = mutableListOf<String>()
        var imported = 0
        var lineNumber = 0

        BufferedReader(InputStreamReader(file.inputStream, StandardCharsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine()
            lineNumber++
            if (headerLine.isNullOrBlank()) {
                throw IllegalArgumentException("CSV file is empty or missing header row")
            }

            var line = reader.readLine()
            while (line != null) {
                lineNumber++
                if (!line.isBlank()) {
                    try {
                        val record = parseLine(line, lineNumber)
                        recordSpendingUseCase.record(record)
                        imported++
                    } catch (e: Exception) {
                        errors.add("line $lineNumber: ${e.message}")
                    }
                }
                line = reader.readLine()
            }
        }

        return CsvImportResponse(imported, errors.size, errors)
    }

    private fun parseLine(line: String, @Suppress("UNUSED_PARAMETER") lineNumber: Int): SpendingRecord {
        val columns = line.split(",")
        if (columns.size < EXPECTED_COLUMNS) {
            throw IllegalArgumentException("expected $EXPECTED_COLUMNS columns but found ${columns.size}")
        }

        val dateStr = columns[0].trim()
        val cardId = columns[1].trim()
        val amountStr = columns[2].trim()
        val merchantName = columns[3].trim()
        val merchantCategory = columns[4].trim()
        val tagsStr = columns[5].trim()

        require(cardId.isNotBlank()) { "cardId is blank" }
        require(merchantName.isNotBlank()) { "merchantName is blank" }

        val date = try { LocalDate.parse(dateStr) }
            catch (e: DateTimeParseException) { throw IllegalArgumentException("invalid date: $dateStr") }

        val amount = try { tagsStr.let { tagsStr }; amountStr.toLong() }
            catch (e: NumberFormatException) { throw IllegalArgumentException("invalid amount: $amountStr") }
        require(amount > 0) { "amount must be positive: $amount" }

        val rawTags = if (tagsStr.isBlank()) emptySet()
        else tagsStr.split("|").toSet()

        val normalized = transactionNormalizer.normalize(
            merchantName,
            merchantCategory.ifBlank { null },
            rawTags,
        )

        return SpendingRecord(
            id = UUID.randomUUID(),
            cardId = CardId(cardId),
            amount = Money.won(amount),
            spentOn = date,
            merchantName = merchantName,
            merchantCategory = normalized.merchantCategory,
            paymentTags = normalized.paymentTags,
        )
    }

    data class CsvImportResponse(val importedCount: Int, val errorCount: Int, val errors: List<String>)

    companion object {
        private const val EXPECTED_COLUMNS = 6
    }
}
