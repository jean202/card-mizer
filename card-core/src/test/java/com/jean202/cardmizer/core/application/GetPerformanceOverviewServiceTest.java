package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.GetPerformanceOverviewUseCase.CardPerformanceSnapshot;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPerformanceOverviewServiceTest {
    @Test
    void returnsMonthlyOverviewSortedByPriority() {
        Card samsung = new Card(new CardId("SAMSUNG_MAIN"), "삼성카드", "대표 카드", CardType.CREDIT, 1);
        Card hyundai = new Card(new CardId("HYUNDAI_MAIN"), "현대카드", "대표 카드", CardType.CREDIT, 2);

        LoadCardCatalogPort cardCatalogPort = () -> List.of(hyundai, samsung);
        LoadCardPerformancePoliciesPort policiesPort = () -> List.of(
                new CardPerformancePolicy(
                        samsung.id(),
                        List.of(
                                new PerformanceTier("S1", Money.won(300_000), "기본 실적 구간"),
                                new PerformanceTier("S2", Money.won(400_000), "상위 실적 구간")
                        )
                ),
                new CardPerformancePolicy(
                        hyundai.id(),
                        List.of(new PerformanceTier("H1", Money.won(300_000), "기본 실적 구간"))
                )
        );
        LoadSpendingRecordsPort spendingRecordsPort = period -> List.of(
                new SpendingRecord(
                        UUID.randomUUID(),
                        samsung.id(),
                        Money.won(120_000),
                        LocalDate.of(2026, 3, 4),
                        "쿠팡",
                        "ONLINE"
                ),
                new SpendingRecord(
                        UUID.randomUUID(),
                        samsung.id(),
                        Money.won(80_000),
                        LocalDate.of(2026, 3, 9),
                        "배달",
                        "FOOD"
                ),
                new SpendingRecord(
                        UUID.randomUUID(),
                        hyundai.id(),
                        Money.won(300_000),
                        LocalDate.of(2026, 3, 11),
                        "주유",
                        "AUTO"
                )
        );

        GetPerformanceOverviewService service = new GetPerformanceOverviewService(
                cardCatalogPort,
                policiesPort,
                spendingRecordsPort
        );

        List<CardPerformanceSnapshot> overview = service.getOverview(new SpendingPeriod(YearMonth.of(2026, 3)));

        assertEquals(2, overview.size());

        CardPerformanceSnapshot first = overview.get(0);
        CardPerformanceSnapshot second = overview.get(1);

        assertAll(
                () -> assertEquals("SAMSUNG_MAIN", first.card().id().value()),
                () -> assertEquals(200_000L, first.spentAmount().amount()),
                () -> assertEquals(300_000L, first.targetAmount().amount()),
                () -> assertEquals(100_000L, first.remainingAmount().amount()),
                () -> assertFalse(first.achieved()),
                () -> assertEquals("S1", first.targetTierCode()),
                () -> assertEquals("HYUNDAI_MAIN", second.card().id().value()),
                () -> assertEquals(300_000L, second.spentAmount().amount()),
                () -> assertEquals(300_000L, second.targetAmount().amount()),
                () -> assertEquals(0L, second.remainingAmount().amount()),
                () -> assertTrue(second.achieved()),
                () -> assertEquals("H1", second.targetTierCode())
        );
    }
}
