package com.jean202.cardmizer.core.domain

import java.time.LocalDate
import java.time.YearMonth

data class SpendingPeriod(val yearMonth: YearMonth) {

    fun includes(date: LocalDate): Boolean = YearMonth.from(date) == yearMonth

    fun previous(): SpendingPeriod = SpendingPeriod(yearMonth.minusMonths(1))

    fun fromStartOfYear(): List<SpendingPeriod> =
        (1..yearMonth.monthValue).map { month ->
            SpendingPeriod(YearMonth.of(yearMonth.year, month))
        }
}
