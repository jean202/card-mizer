package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.GetPerformanceOverviewUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetPerformanceOverviewService implements GetPerformanceOverviewUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort;
    private final LoadSpendingRecordsPort loadSpendingRecordsPort;

    public GetPerformanceOverviewService(
            LoadCardCatalogPort loadCardCatalogPort,
            LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort,
            LoadSpendingRecordsPort loadSpendingRecordsPort
    ) {
        this.loadCardCatalogPort = Objects.requireNonNull(loadCardCatalogPort, "loadCardCatalogPort must not be null");
        this.loadCardPerformancePoliciesPort = Objects.requireNonNull(
                loadCardPerformancePoliciesPort,
                "loadCardPerformancePoliciesPort must not be null"
        );
        this.loadSpendingRecordsPort = Objects.requireNonNull(
                loadSpendingRecordsPort,
                "loadSpendingRecordsPort must not be null"
        );
    }

    @Override
    public List<CardPerformanceSnapshot> getOverview(SpendingPeriod period) {
        Objects.requireNonNull(period, "period must not be null");

        Map<CardId, CardPerformancePolicy> policiesByCardId = loadCardPerformancePoliciesPort.loadAll().stream()
                .collect(Collectors.toMap(CardPerformancePolicy::cardId, Function.identity()));
        Map<CardId, Money> spentByCardId = aggregateSpentAmounts(loadSpendingRecordsPort.loadByPeriod(period));

        return loadCardCatalogPort.loadAll().stream()
                .sorted(Comparator.comparingInt(Card::priority))
                .map(card -> toSnapshot(card, policiesByCardId.get(card.id()), spentByCardId.getOrDefault(card.id(), Money.ZERO)))
                .toList();
    }

    private Map<CardId, Money> aggregateSpentAmounts(List<SpendingRecord> spendingRecords) {
        Map<CardId, Money> spentByCardId = new HashMap<>();
        for (SpendingRecord spendingRecord : spendingRecords) {
            spentByCardId.merge(spendingRecord.cardId(), spendingRecord.amount(), Money::add);
        }
        return spentByCardId;
    }

    private CardPerformanceSnapshot toSnapshot(Card card, CardPerformancePolicy policy, Money spentAmount) {
        if (policy == null) {
            throw new IllegalStateException("No performance policy configured for card: " + card.id().value());
        }

        boolean achieved = policy.isFullyAchieved(spentAmount);
        PerformanceTier targetTier = policy.nextTier(spentAmount).orElse(policy.highestTier());
        Money targetAmount = targetTier.targetAmount();
        Money remainingAmount = achieved ? Money.ZERO : targetAmount.subtract(spentAmount);

        return new CardPerformanceSnapshot(
                card,
                spentAmount,
                targetAmount,
                remainingAmount,
                achieved,
                targetTier.code()
        );
    }
}
