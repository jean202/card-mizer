package com.jean202.cardmizer.infra.persistence

import com.jean202.cardmizer.core.application.ResourceNotFoundException
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class InMemorySpendingRecordAdapter(
    initialSpendingRecords: List<SpendingRecord> = emptyList(),
) : LoadSpendingRecordsPort, SaveSpendingRecordPort, LoadSpendingRecordsByCardAndPeriodPort, DeleteSpendingRecordPort {

    private val spendingRecords = CopyOnWriteArrayList(initialSpendingRecords)
    private val deletedIds = mutableSetOf<UUID>()

    override fun loadByPeriod(period: SpendingPeriod): List<SpendingRecord> =
        spendingRecords
            .filter { it.id !in deletedIds && period.includes(it.spentOn) }
            .sortedWith(compareBy({ it.spentOn }, { it.id }))

    override fun loadByPeriodAndCard(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord> =
        spendingRecords
            .filter { it.id !in deletedIds && period.includes(it.spentOn) && (cardId == null || it.cardId == cardId) }
            .sortedWith(compareByDescending<SpendingRecord> { it.spentOn }.thenByDescending { it.id })

    override fun save(spendingRecord: SpendingRecord) {
        spendingRecords.add(spendingRecord)
    }

    override fun delete(id: UUID) {
        val exists = spendingRecords.any { it.id == id }
        if (!exists || id in deletedIds) {
            throw ResourceNotFoundException("Spending record not found: $id")
        }
        deletedIds.add(id)
    }
}
