package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.PriorityStrategy

fun interface UpdatePriorityUseCase {
    fun update(priorityStrategy: PriorityStrategy)
}
