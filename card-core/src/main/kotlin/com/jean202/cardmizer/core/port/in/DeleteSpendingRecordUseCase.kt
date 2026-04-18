package com.jean202.cardmizer.core.port.`in`

import java.util.UUID

fun interface DeleteSpendingRecordUseCase {
    fun delete(id: UUID)
}
