package com.jean202.cardmizer.api.sync

import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase
import com.jean202.cardmizer.core.port.`in`.SyncCardTransactionsUseCase.SyncResult
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncCardTransactionsUseCase: SyncCardTransactionsUseCase,
) {
    @PostMapping("/transactions")
    fun sync(@Valid @RequestBody request: SyncRequest): SyncResponse {
        val period = SpendingPeriod(YearMonth.parse(request.yearMonth))
        val result = syncCardTransactionsUseCase.sync(period)
        return SyncResponse.from(result)
    }

    data class SyncRequest(
        @field:NotBlank(message = "yearMonth must not be blank")
        @field:Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth must be in yyyy-MM format")
        val yearMonth: String,
    )

    data class SyncResponse(val fetchedCount: Int, val savedCount: Int, val syncedCardIds: List<String>) {
        companion object {
            fun from(result: SyncResult) = SyncResponse(result.fetchedCount, result.savedCount, result.syncedCardIds)
        }
    }
}
