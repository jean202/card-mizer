package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort

class RecordSpendingService(
    private val saveSpendingRecordPort: SaveSpendingRecordPort,
) : RecordSpendingUseCase {

    override fun record(spendingRecord: SpendingRecord) {
        saveSpendingRecordPort.save(spendingRecord)
    }
}
