package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.port.`in`.DeleteSpendingRecordUseCase
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort
import java.util.UUID

class DeleteSpendingRecordService(
    private val deleteSpendingRecordPort: DeleteSpendingRecordPort,
) : DeleteSpendingRecordUseCase {

    override fun delete(id: UUID) {
        deleteSpendingRecordPort.delete(id)
    }
}
