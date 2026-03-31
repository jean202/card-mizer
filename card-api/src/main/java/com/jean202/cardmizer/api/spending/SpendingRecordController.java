package com.jean202.cardmizer.api.spending;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spending-records")
public class SpendingRecordController {
    private final RecordSpendingUseCase recordSpendingUseCase;
    private final TransactionNormalizer transactionNormalizer;

    public SpendingRecordController(
            RecordSpendingUseCase recordSpendingUseCase,
            TransactionNormalizer transactionNormalizer
    ) {
        this.recordSpendingUseCase = recordSpendingUseCase;
        this.transactionNormalizer = transactionNormalizer;
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
}
