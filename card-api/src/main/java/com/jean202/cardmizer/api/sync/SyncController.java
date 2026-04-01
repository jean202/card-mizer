package com.jean202.cardmizer.api.sync;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase;
import com.jean202.cardmizer.core.port.in.SyncCardTransactionsUseCase.SyncResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final SyncCardTransactionsUseCase syncCardTransactionsUseCase;

    public SyncController(SyncCardTransactionsUseCase syncCardTransactionsUseCase) {
        this.syncCardTransactionsUseCase = syncCardTransactionsUseCase;
    }

    @PostMapping("/transactions")
    public SyncResponse sync(@Valid @RequestBody SyncRequest request) {
        SpendingPeriod period = new SpendingPeriod(YearMonth.parse(request.yearMonth()));
        SyncResult result = syncCardTransactionsUseCase.sync(period);
        return SyncResponse.from(result);
    }

    public record SyncRequest(
            @NotBlank(message = "yearMonth must not be blank")
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth must be in yyyy-MM format")
            String yearMonth
    ) {}

    public record SyncResponse(int fetchedCount, int savedCount, List<String> syncedCardIds) {
        static SyncResponse from(SyncResult result) {
            return new SyncResponse(result.fetchedCount(), result.savedCount(), result.syncedCardIds());
        }
    }
}
