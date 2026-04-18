package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface FetchCardTransactionsPort {
    fun fetchByCardAndPeriod(cardId: CardId, period: SpendingPeriod): List<SpendingRecord>
}
