package com.jean202.cardmizer.api.spending;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.DeleteSpendingRecordUseCase;
import com.jean202.cardmizer.core.port.in.GetSpendingRecordsUseCase;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spending-records")
public class SpendingRecordController {
    private final RecordSpendingUseCase recordSpendingUseCase;
    private final GetSpendingRecordsUseCase getSpendingRecordsUseCase;
    private final DeleteSpendingRecordUseCase deleteSpendingRecordUseCase;
    private final TransactionNormalizer transactionNormalizer;
    private final Clock clock;

    public SpendingRecordController(
            RecordSpendingUseCase recordSpendingUseCase,
            GetSpendingRecordsUseCase getSpendingRecordsUseCase,
            DeleteSpendingRecordUseCase deleteSpendingRecordUseCase,
            TransactionNormalizer transactionNormalizer,
            Clock clock
    ) {
        this.recordSpendingUseCase = recordSpendingUseCase;
        this.getSpendingRecordsUseCase = getSpendingRecordsUseCase;
        this.deleteSpendingRecordUseCase = deleteSpendingRecordUseCase;
        this.transactionNormalizer = transactionNormalizer;
        this.clock = clock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateSpendingRecordRequest request) {
        NormalizedTransaction normalizedTransaction = transactionNormalizer.normalize(
                request.merchantName(),
                request.merchantCategory(),
                request.paymentTags()
        );
        recordSpendingUseCase.record(request.toDomain(normalizedTransaction));
    }

    @GetMapping
    public SpendingRecordsResponse list(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String cardId
    ) {
        YearMonth period = yearMonth != null
                ? YearMonth.parse(yearMonth)
                : YearMonth.now(clock);
        CardId filterCardId = cardId != null && !cardId.isBlank() ? new CardId(cardId) : null;
        List<SpendingRecord> records = getSpendingRecordsUseCase.getByPeriod(
                new SpendingPeriod(period), filterCardId);
        List<SpendingRecordResponse> responses = records.stream()
                .map(SpendingRecordResponse::from)
                .toList();
        return new SpendingRecordsResponse(responses, responses.size(), period.toString());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteSpendingRecordUseCase.delete(id);
    }

    public record CreateSpendingRecordRequest(
            @NotBlank(message = "cardId must not be blank")
            String cardId,
            @Positive(message = "amount must be greater than 0")
            long amount,
            @NotNull(message = "spentOn must not be null")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate spentOn,
            @NotBlank(message = "merchantName must not be blank")
            String merchantName,
            String merchantCategory,
            Set<@NotBlank(message = "paymentTags must not contain blank values") String> paymentTags
    ) {
        SpendingRecord toDomain(NormalizedTransaction normalizedTransaction) {
            return new SpendingRecord(
                    UUID.randomUUID(),
                    new CardId(cardId),
                    Money.won(amount),
                    spentOn,
                    merchantName,
                    normalizedTransaction.merchantCategory(),
                    normalizedTransaction.paymentTags()
            );
        }
    }

    public record SpendingRecordsResponse(
            List<SpendingRecordResponse> records,
            int count,
            String yearMonth
    ) {
    }

    public record SpendingRecordResponse(
            UUID id,
            String cardId,
            long amount,
            LocalDate spentOn,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags
    ) {
        static SpendingRecordResponse from(SpendingRecord record) {
            return new SpendingRecordResponse(
                    record.id(),
                    record.cardId().value(),
                    record.amount().amount(),
                    record.spentOn(),
                    record.merchantName(),
                    record.merchantCategory(),
                    record.paymentTags()
            );
        }
    }
}
