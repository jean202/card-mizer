package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.GetSpendingRecordsUseCase
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsByCardAndPeriodPort

class GetSpendingRecordsService(
    private val loadSpendingRecordsByCardAndPeriodPort: LoadSpendingRecordsByCardAndPeriodPort,
) : GetSpendingRecordsUseCase {

    override fun getByPeriod(period: SpendingPeriod, cardId: CardId?): List<SpendingRecord> =
        loadSpendingRecordsByCardAndPeriodPort.loadByPeriodAndCard(period, cardId)
}
