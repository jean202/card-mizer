package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface SaveSpendingRecordPort {
    fun save(spendingRecord: SpendingRecord)
}
