package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitQuote;
import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.domain.RecommendationCandidate;
import com.jean202.cardmizer.core.domain.RecommendationContext;
import com.jean202.cardmizer.core.domain.RecommendationResult;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecommendCardUseCase;
import com.jean202.cardmizer.core.port.out.LoadCardCatalogPort;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecommendCardService implements RecommendCardUseCase {
    private final LoadCardCatalogPort loadCardCatalogPort;
    private final LoadCardPerformancePoliciesPort loadCardPerformancePoliciesPort;
    private final LoadSpendingRecordsPort loadSpendingRecordsPort;

    public RecommendCardService(
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
    public RecommendationResult recommend(RecommendationContext context) {
        Objects.requireNonNull(context, "context must not be null");

        List<SpendingRecord> spendingRecords = loadSpendingRecordsPort.loadByPeriod(context.spendingPeriod());
        List<SpendingRecord> previousMonthRecords = loadSpendingRecordsPort.loadByPeriod(context.spendingPeriod().previous());
        List<SpendingRecord> yearToDateRecords = loadYearToDateRecords(context);
        Map<CardId, CardPerformancePolicy> policiesByCardId = loadCardPerformancePoliciesPort.loadAll().stream()
                .collect(Collectors.toMap(CardPerformancePolicy::cardId, Function.identity()));
        Map<CardId, Money> spentByCardId = aggregateSpentAmounts(spendingRecords);
        Map<CardId, Money> previousMonthSpentByCardId = aggregateSpentAmounts(previousMonthRecords);
        Map<CardId, List<SpendingRecord>> recordsByCardId = spendingRecords.stream()
                .collect(Collectors.groupingBy(SpendingRecord::cardId));
        Map<CardId, List<SpendingRecord>> yearToDateRecordsByCardId = yearToDateRecords.stream()
                .collect(Collectors.groupingBy(SpendingRecord::cardId));

        List<ScoredRecommendation> scoredRecommendations = loadCardCatalogPort.loadAll().stream()
                .map(card -> score(
                        card,
                        policiesByCardId.get(card.id()),
                        spentByCardId.getOrDefault(card.id(), Money.ZERO),
                        previousMonthSpentByCardId.getOrDefault(card.id(), Money.ZERO),
                        recordsByCardId.getOrDefault(card.id(), List.of()),
                        yearToDateRecordsByCardId.getOrDefault(card.id(), List.of()),
                        context
                ))
                .sorted(Comparator
                        .comparingInt(ScoredRecommendation::score).reversed()
                        .thenComparing(scored -> scored.card().priority()))
                .toList();

        if (scoredRecommendations.isEmpty()) {
            throw new IllegalStateException("No cards configured for recommendation");
        }

        ScoredRecommendation topRecommendation = scoredRecommendations.get(0);
        List<RecommendationCandidate> alternatives = scoredRecommendations.stream()
                .skip(1)
                .map(scored -> new RecommendationCandidate(scored.card(), scored.reason(), scored.score()))
                .toList();

        return new RecommendationResult(
                topRecommendation.card(),
                topRecommendation.reason(),
                alternatives
        );
    }

    private Map<CardId, Money> aggregateSpentAmounts(List<SpendingRecord> spendingRecords) {
        Map<CardId, Money> spentByCardId = new HashMap<>();
        for (SpendingRecord spendingRecord : spendingRecords) {
            spentByCardId.merge(spendingRecord.cardId(), spendingRecord.amount(), Money::add);
        }
        return spentByCardId;
    }

    private ScoredRecommendation score(
            Card card,
            CardPerformancePolicy policy,
            Money currentSpentAmount,
            Money previousMonthSpentAmount,
            List<SpendingRecord> currentMonthRecords,
            List<SpendingRecord> currentYearRecords,
            RecommendationContext context
    ) {
        if (policy == null) {
            throw new IllegalStateException("No performance policy configured for card: " + card.id().value());
        }

        Money projectedSpentAmount = currentSpentAmount.add(context.amount());
        Optional<PerformanceTier> nextTierBefore = policy.nextTier(currentSpentAmount);
        Optional<BenefitQuote> benefitQuote = policy.estimateBenefit(
                context.amount(),
                context.merchantName(),
                context.merchantCategory(),
                context.paymentTags(),
                previousMonthSpentAmount,
                currentMonthRecords,
                currentYearRecords
        );
        boolean reachesNextTier = nextTierBefore
                .map(tier -> projectedSpentAmount.isGreaterThanOrEqual(tier.targetAmount()))
                .orElse(false);
        boolean alreadyFullyAchieved = policy.isFullyAchieved(currentSpentAmount);

        int score = computeScore(
                card,
                policy,
                currentSpentAmount,
                projectedSpentAmount,
                reachesNextTier,
                alreadyFullyAchieved,
                benefitQuote
        );
        String reason = buildReason(
                card,
                currentSpentAmount,
                projectedSpentAmount,
                nextTierBefore,
                reachesNextTier,
                alreadyFullyAchieved,
                benefitQuote
        );

        return new ScoredRecommendation(card, score, reason);
    }

    private int computeScore(
            Card card,
            CardPerformancePolicy policy,
            Money currentSpentAmount,
            Money projectedSpentAmount,
            boolean reachesNextTier,
            boolean alreadyFullyAchieved,
            Optional<BenefitQuote> benefitQuote
    ) {
        int priorityBonus = Math.max(0, 100 - card.priority());
        int benefitBonus = (int) Math.min(
                benefitQuote.map(quote -> quote.benefitAmount().amount() / 10L).orElse(0L),
                1_500L
        );
        if (reachesNextTier) {
            long overshoot = projectedSpentAmount.amount() - policy.nextTier(currentSpentAmount).orElseThrow().targetAmount().amount();
            int overshootPenalty = (int) Math.min(overshoot / 1_000L, 200L);
            return 10_000 + priorityBonus - overshootPenalty + benefitBonus;
        }

        if (alreadyFullyAchieved) {
            return 1_000 + priorityBonus + benefitBonus;
        }

        PerformanceTier nextTier = policy.nextTier(currentSpentAmount).orElseThrow();
        long remainingAfterPayment = Math.max(0L, nextTier.targetAmount().amount() - projectedSpentAmount.amount());
        int remainingPenalty = (int) Math.min(remainingAfterPayment / 1_000L, 5_000L);
        return 5_000 + priorityBonus - remainingPenalty + benefitBonus;
    }

    private String buildReason(
            Card card,
            Money currentSpentAmount,
            Money projectedSpentAmount,
            Optional<PerformanceTier> nextTierBefore,
            boolean reachesNextTier,
            boolean alreadyFullyAchieved,
            Optional<BenefitQuote> benefitQuote
    ) {
        String benefitSuffix = benefitQuote
                .map(this::formatBenefitSuffix)
                .orElse("");
        if (reachesNextTier) {
            PerformanceTier nextTier = nextTierBefore.orElseThrow();
            long remainingBeforePayment = nextTier.targetAmount().amount() - currentSpentAmount.amount();
            return "%s %s 구간까지 %,d원 남아 있어 이번 결제로 바로 달성할 수 있습니다."
                    .formatted(card.displayName(), nextTier.code(), remainingBeforePayment)
                    + benefitSuffix;
        }

        if (alreadyFullyAchieved) {
            return "%s는 이미 최고 실적 구간을 달성한 상태라, 이번 결제는 우선순위 기준으로만 추천합니다."
                    .formatted(card.displayName())
                    + benefitSuffix;
        }

        PerformanceTier nextTier = nextTierBefore.orElseThrow();
        long remainingAfterPayment = Math.max(0L, nextTier.targetAmount().amount() - projectedSpentAmount.amount());
        return "%s는 이번 결제 후 %s 구간까지 %,d원이 남아 다음 실적 구간에 가장 가깝습니다."
                .formatted(card.displayName(), nextTier.code(), remainingAfterPayment)
                + benefitSuffix;
    }

    private String formatBenefitSuffix(BenefitQuote quote) {
        String capSuffix = quote.wasCapped()
                ? " 월 한도로 일부만 반영되었습니다."
                : "";
        return " 예상 혜택은 %,d원(%s)입니다.%s"
                .formatted(quote.benefitAmount().amount(), quote.summary(), capSuffix);
    }

    private List<SpendingRecord> loadYearToDateRecords(RecommendationContext context) {
        return context.spendingPeriod().fromStartOfYear().stream()
                .map(loadSpendingRecordsPort::loadByPeriod)
                .flatMap(List::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                SpendingRecord::id,
                                Function.identity(),
                                (left, right) -> left,
                                java.util.LinkedHashMap::new
                        ),
                        recordsById -> List.copyOf(recordsById.values())
                ));
    }

    private record ScoredRecommendation(Card card, int score, String reason) {
    }
}
