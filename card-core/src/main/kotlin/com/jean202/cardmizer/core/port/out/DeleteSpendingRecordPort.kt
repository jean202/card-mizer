package com.jean202.cardmizer.core.port.out

import java.util.UUID

fun interface DeleteSpendingRecordPort {
    fun delete(id: UUID)
}
