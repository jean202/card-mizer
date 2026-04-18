package com.jean202.cardmizer.infra.file

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
@Profile("file-sync")
class FileBasedCardTransactionsAdapter(
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    @Value("\${cardcompany.file.directory:classpath:transactions}") private val directory: String,
) : FetchCardTransactionsPort {

    override fun fetchByCardAndPeriod(cardId: CardId, period: SpendingPeriod): List<SpendingRecord> {
        val fileName = "${cardId.value}-${period.yearMonth}.json"
        val resource = resourceLoader.getResource("$directory/$fileName")

        if (!resource.exists()) return emptyList()

        return try {
            resource.inputStream.use { inputStream ->
                val dtos = objectMapper.readValue(
                    inputStream,
                    object : TypeReference<List<FileTransactionDto>>() {},
                )
                dtos.map { toDomain(it) }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read transaction file: $fileName", e)
        }
    }

    private fun toDomain(dto: FileTransactionDto) = SpendingRecord(
        id = UUID.randomUUID(),
        cardId = CardId(dto.card),
        amount = Money.won(dto.won),
        spentOn = LocalDate.parse(dto.date),
        merchantName = dto.store,
        merchantCategory = dto.category ?: "UNCATEGORIZED",
        paymentTags = dto.tags?.toHashSet() ?: emptySet(),
    )

    private data class FileTransactionDto(
        val date: String,
        val store: String,
        val category: String?,
        val won: Long,
        val card: String,
        val tags: List<String>?,
    )
}
