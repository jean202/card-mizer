package com.jean202.cardmizer.core.port.out

import com.jean202.cardmizer.core.domain.Card

fun interface LoadCardCatalogPort {
    fun loadAll(): List<Card>
}
