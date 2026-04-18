package com.jean202.cardmizer.core.port.`in`

import com.jean202.cardmizer.core.domain.Card

fun interface GetCardsUseCase {
    fun getAll(): List<Card>
}
