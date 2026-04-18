package com.jean202.cardmizer.infra.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface JpaSpendingRecordRepository : JpaRepository<JpaSpendingRecordEntity, UUID> {
    fun findByDeletedFalseAndSpentOnBetweenOrderBySpentOnAscIdAsc(
        startDate: LocalDate, endDate: LocalDate,
    ): List<JpaSpendingRecordEntity>

    fun findByDeletedFalseAndSpentOnBetweenOrderBySpentOnDescIdDesc(
        startDate: LocalDate, endDate: LocalDate,
    ): List<JpaSpendingRecordEntity>

    fun findByDeletedFalseAndCardIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(
        cardId: String, startDate: LocalDate, endDate: LocalDate,
    ): List<JpaSpendingRecordEntity>
}
