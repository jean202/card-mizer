package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface RecordSpendingUseCase {
    fun record(spendingRecord: SpendingRecord)
}
