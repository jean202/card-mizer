package com.jean202.cardmizer.api.performance

import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.port.`in`.GetPerformanceOverviewUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/performance-overview")
class PerformanceOverviewController(
    private val getPerformanceOverviewUseCase: GetPerformanceOverviewUseCase,
) {
    @GetMapping
    fun getOverview(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") yearMonth: YearMonth,
    ): List<PerformanceOverviewResponse> =
        getPerformanceOverviewUseCase.getOverview(SpendingPeriod(yearMonth))
            .map { PerformanceOverviewResponse.from(it) }

    data class PerformanceOverviewResponse(
        val cardId: String,
        val cardName: String,
        val priority: Int,
        val spentAmount: Long,
        val targetAmount: Long,
        val remainingAmount: Long,
        val achieved: Boolean,
        val targetTierCode: String,
    ) {
        companion object {
            fun from(snapshot: GetPerformanceOverviewUseCase.CardPerformanceSnapshot) =
                PerformanceOverviewResponse(
                    cardId = snapshot.card.id.value,
                    cardName = snapshot.card.displayName(),
                    priority = snapshot.card.priority,
                    spentAmount = snapshot.spentAmount.amount,
                    targetAmount = snapshot.targetAmount.amount,
                    remainingAmount = snapshot.remainingAmount.amount,
                    achieved = snapshot.achieved,
                    targetTierCode = snapshot.targetTierCode,
                )
        }
    }
}
