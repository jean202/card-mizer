package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.port.`in`.GetCardsUseCase
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort

class GetCardsService(
    private val loadCardCatalogPort: LoadCardCatalogPort,
) : GetCardsUseCase {

    override fun getAll(): List<Card> =
        loadCardCatalogPort.loadAll().sortedBy { it.priority }
}
