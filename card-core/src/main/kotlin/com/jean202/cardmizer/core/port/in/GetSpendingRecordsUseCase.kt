package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface GetSpendingRecordsUseCase {
    fun getByPeriod(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord>
}
