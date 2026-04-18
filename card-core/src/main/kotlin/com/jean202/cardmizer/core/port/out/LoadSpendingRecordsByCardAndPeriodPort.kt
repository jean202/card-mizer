package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface LoadSpendingRecordsByCardAndPeriodPort {
    fun loadByPeriodAndCard(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord>
}
