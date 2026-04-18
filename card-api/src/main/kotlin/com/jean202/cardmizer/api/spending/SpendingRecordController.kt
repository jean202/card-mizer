package com.jean202.cardmizer.api.spending

import com.fasterxml.jackson.annotation.JsonFormat
import com.jean202.cardmizer.api.normalization.NormalizedTransaction
import com.jean202.cardmizer.api.normalization.TransactionNormalizer
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.`in`.DeleteSpendingRecordUseCase
import com.jean202.cardmizer.core.port.`in`.GetSpendingRecordsUseCase
import com.jean202.cardmizer.core.port.`in`.RecordSpendingUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/spending-records")
class SpendingRecordController(
    private val recordSpendingUseCase: RecordSpendingUseCase,
    private val getSpendingRecordsUseCase: GetSpendingRecordsUseCase,
    private val deleteSpendingRecordUseCase: DeleteSpendingRecordUseCase,
    private val transactionNormalizer: TransactionNormalizer,
    private val clock: Clock,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateSpendingRecordRequest) {
        val normalized = transactionNormalizer.normalize(request.merchantName, request.merchantCategory, request.paymentTags)
        recordSpendingUseCase.record(request.toDomain(normalized))
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) cardId: String?,
    ): SpendingRecordsResponse {
        val period = if (yearMonth != null) YearMonth.parse(yearMonth) else YearMonth.now(clock)
        val filterCardId = if (!cardId.isNullOrBlank()) CardId(cardId) else null
        val records = getSpendingRecordsUseCase.getByPeriod(SpendingPeriod(period), filterCardId)
        val responses = records.map { SpendingRecordResponse.from(it) }
        return SpendingRecordsResponse(responses, responses.size, period.toString())
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        deleteSpendingRecordUseCase.delete(id)
    }

    data class CreateSpendingRecordRequest(
        @field:NotBlank(message = "cardId must not be blank")
        val cardId: String,
        @field:Positive(message = "amount must be greater than 0")
        val amount: Long,
        @field:NotNull(message = "spentOn must not be null")
        @field:JsonFormat(pattern = "yyyy-MM-dd")
        val spentOn: LocalDate,
        @field:NotBlank(message = "merchantName must not be blank")
        val merchantName: String,
        val merchantCategory: String?,
        val paymentTags: Set<@NotBlank(message = "paymentTags must not contain blank values") String>?,
    ) {
        fun toDomain(normalized: NormalizedTransaction) = SpendingRecord(
            id = UUID.randomUUID(),
            cardId = CardId(cardId),
            amount = Money.won(amount),
            spentOn = spentOn,
            merchantName = merchantName,
            merchantCategory = normalized.merchantCategory,
            paymentTags = normalized.paymentTags,
        )
    }

    data class SpendingRecordsResponse(
        val records: List<SpendingRecordResponse>,
        val count: Int,
        val yearMonth: String,
    )

    data class SpendingRecordResponse(
        val id: UUID,
        val cardId: String,
        val amount: Long,
        val spentOn: LocalDate,
        val merchantName: String,
        val merchantCategory: String,
        val paymentTags: Set<String>,
    ) {
        companion object {
            fun from(record: SpendingRecord) = SpendingRecordResponse(
                id = record.id,
                cardId = record.cardId.value,
                amount = record.amount.amount,
                spentOn = record.spentOn,
                merchantName = record.merchantName,
                merchantCategory = record.merchantCategory,
                paymentTags = record.paymentTags,
            )
        }
    }
}
