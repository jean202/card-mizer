package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase.SyncResult
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort

class SyncCardTransactionsService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
    private val fetchCardTransactionsPort: FetchCardTransactionsPort,
    private val saveSpendingRecordPort: SaveSpendingRecordPort,
) : SyncCardTransactionsUseCase {

    override fun sync(period: SpendingPeriod): SyncResult {
        val cards = loadCardCatalogPort.loadAll()
        var fetchedCount = 0
        var savedCount = 0
        val syncedCardIds = mutableListOf<String>()

        for (card in cards) {
            val fetched = fetchCardTransactionsPort.fetchByCardAndPeriod(card.id, period)
            fetchedCount += fetched.size
            fetched.forEach { record ->
                saveSpendingRecordPort.save(record)
                savedCount++
            }
            syncedCardIds.add(card.id.value)
        }

        return SyncResult(fetchedCount, savedCount, syncedCardIds)
    }
}
