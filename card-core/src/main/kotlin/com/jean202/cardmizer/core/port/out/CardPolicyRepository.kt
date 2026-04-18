package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.Card

fun interface CardPolicyRepository {
    fun findAll(): List<Card>
}
