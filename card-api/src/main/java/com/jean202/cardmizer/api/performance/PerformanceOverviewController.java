package com.jean202.cardmizer.api.performance;

import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.port.in.GetPerformanceOverviewUseCase;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance-overview")
public class PerformanceOverviewController {
    private final GetPerformanceOverviewUseCase getPerformanceOverviewUseCase;

    public PerformanceOverviewController(GetPerformanceOverviewUseCase getPerformanceOverviewUseCase) {
        this.getPerformanceOverviewUseCase = getPerformanceOverviewUseCase;
    }

    @GetMapping
    public List<PerformanceOverviewResponse> getOverview(
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth yearMonth
    ) {
        return getPerformanceOverviewUseCase.getOverview(new SpendingPeriod(yearMonth)).stream()
                .map(PerformanceOverviewResponse::from)
                .toList();
    }

    public record PerformanceOverviewResponse(
            String cardId,
            String cardName,
            int priority,
            long spentAmount,
            long targetAmount,
            long remainingAmount,
            boolean achieved,
            String targetTierCode
    ) {
        static PerformanceOverviewResponse from(GetPerformanceOverviewUseCase.CardPerformanceSnapshot snapshot) {
            return new PerformanceOverviewResponse(
                    snapshot.card().id().value(),
                    snapshot.card().displayName(),
                    snapshot.card().priority(),
                    snapshot.spentAmount().amount(),
                    snapshot.targetAmount().amount(),
                    snapshot.remainingAmount().amount(),
                    snapshot.achieved(),
                    snapshot.targetTierCode()
            );
        }
    }
}
