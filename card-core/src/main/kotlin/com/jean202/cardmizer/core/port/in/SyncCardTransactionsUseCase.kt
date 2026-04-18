package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.SpendingPeriod

interface SyncCardTransactionsUseCase {
    fun sync(period: SpendingPeriod): SyncResult

    data class SyncResult(
        val fetchedCount: Int,
        val savedCount: Int,
        val syncedCardIds: List<String>,
    ) {
        init {
            require(fetchedCount >= 0) { "fetchedCount must not be negative" }
            require(savedCount >= 0) { "savedCount must not be negative" }
        }
    }
}
