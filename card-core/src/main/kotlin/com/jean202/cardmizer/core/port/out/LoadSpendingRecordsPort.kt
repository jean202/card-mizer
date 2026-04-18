package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord

fun interface LoadSpendingRecordsPort {
    fun loadByPeriod(period: SpendingPeriod): List<SpendingRecord>
}
