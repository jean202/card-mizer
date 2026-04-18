package com.jean202.cardmizer.infra.persistence.jpa

import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.application.ResourceNotFoundException
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Primary
@Transactional
class JpaSpendingRecordAdapter(
    private val repository: JpaSpendingRecordRepository,
) : LoadSpendingRecordsPort, SaveSpendingRecordPort, LoadSpendingRecordsByCardAndPeriodPort, DeleteSpendingRecordPort {

    @Transactional(readOnly = true)
    override fun loadByPeriod(period: SpendingPeriod): List<SpendingRecord> {
        val startDate = period.yearMonth.atDay(1)
        val endDate = period.yearMonth.atEndOfMonth()
        return repository.findByDeletedFalseAndSpentOnBetweenOrderBySpentOnAscIdAsc(startDate, endDate)
            .map { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun loadByPeriodAndCard(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord> {
        val startDate = period.yearMonth.atDay(1)
        val endDate = period.yearMonth.atEndOfMonth()
        return if (cardId != null) {
            repository.findByDeletedFalseAndCardIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(
                cardId.value, startDate, endDate,
            )
        } else {
            repository.findByDeletedFalseAndSpentOnBetweenOrderBySpentOnDescIdDesc(startDate, endDate)
        }.map { toDomain(it) }
    }

    override fun save(spendingRecord: SpendingRecord) {
        repository.save(toEntity(spendingRecord))
    }

    override fun delete(id: UUID) {
        val entity = repository.findById(id)
            .filter { !it.deleted }
            .orElseThrow { ResourceNotFoundException("Spending record not found: $id") }
        entity.deleted = true
        repository.save(entity)
    }

    private fun toDomain(entity: JpaSpendingRecordEntity) = SpendingRecord(
        id = entity.id,
        cardId = CardId(entity.cardId),
        amount = Money.won(entity.amount),
        spentOn = entity.spentOn,
        merchantName = entity.merchantName,
        merchantCategory = entity.merchantCategory,
        paymentTags = parseTags(entity.paymentTags),
    )

    private fun toEntity(record: SpendingRecord) = JpaSpendingRecordEntity(
        id = record.id,
        cardId = record.cardId.value,
        amount = record.amount.amount,
        spentOn = record.spentOn,
        merchantName = record.merchantName,
        merchantCategory = record.merchantCategory,
        paymentTags = formatTags(record.paymentTags),
        deleted = false,
    )

    private fun formatTags(paymentTags: Set<String>): String =
        paymentTags.sorted().joinToString(",")

    private fun parseTags(serialized: String): Set<String> =
        if (serialized.isBlank()) emptySet()
        else serialized.split(",").filter { it.isNotBlank() }.toSet()
}
