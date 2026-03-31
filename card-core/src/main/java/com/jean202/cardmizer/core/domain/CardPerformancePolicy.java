package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record CardPerformancePolicy(CardId cardId, List<PerformanceTier> tiers, List<BenefitRule> benefitRules) {
    public CardPerformancePolicy(CardId cardId, List<PerformanceTier> tiers) {
        this(cardId, tiers, List.of());
    }

    public CardPerformancePolicy {
        Objects.requireNonNull(cardId, "cardId must not be null");
        tiers = List.copyOf(Objects.requireNonNull(tiers, "tiers must not be null"));
        benefitRules = List.copyOf(Objects.requireNonNull(benefitRules, "benefitRules must not be null"));
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("At least one performance tier is required");
        }
    }

    public Optional<PerformanceTier> nextTier(Money spentAmount) {
        Objects.requireNonNull(spentAmount, "spentAmount must not be null");
        return sortedTiers().stream()
                .filter(tier -> tier.targetAmount().compareTo(spentAmount) > 0)
                .findFirst();
    }

    public PerformanceTier highestTier() {
        return sortedTiers().get(sortedTiers().size() - 1);
    }

    public boolean isFullyAchieved(Money spentAmount) {
        return nextTier(spentAmount).isEmpty();
    }

    public Optional<BenefitQuote> estimateBenefit(
            Money amount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags,
            Money previousMonthSpentAmount,
            List<SpendingRecord> currentMonthRecords,
            List<SpendingRecord> currentYearRecords
    ) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(merchantName, "merchantName must not be null");
        Objects.requireNonNull(merchantCategory, "merchantCategory must not be null");
        Objects.requireNonNull(previousMonthSpentAmount, "previousMonthSpentAmount must not be null");
        List<SpendingRecord> normalizedCurrentMonthRecords = List.copyOf(
                Objects.requireNonNull(currentMonthRecords, "currentMonthRecords must not be null")
        );
        List<SpendingRecord> normalizedCurrentYearRecords = List.copyOf(
                Objects.requireNonNull(currentYearRecords, "currentYearRecords must not be null")
        );

        EvaluationState state = simulateHistory(previousMonthSpentAmount, normalizedCurrentMonthRecords, normalizedCurrentYearRecords);
        List<AppliedBenefit> appliedBenefits = evaluateTransaction(
                amount,
                merchantName,
                merchantCategory,
                paymentTags == null ? Set.of() : paymentTags,
                previousMonthSpentAmount,
                state
        );
        if (appliedBenefits.isEmpty()) {
            return Optional.empty();
        }

        long appliedAmount = appliedBenefits.stream()
                .map(AppliedBenefit::benefitAmount)
                .mapToLong(Money::amount)
                .sum();
        long rawAmount = appliedBenefits.stream()
                .map(AppliedBenefit::rawBenefitAmount)
                .mapToLong(Money::amount)
                .sum();
        return Optional.of(new BenefitQuote(
                appliedBenefits,
                Money.won(appliedAmount),
                Money.won(rawAmount)
        ));
    }

    private EvaluationState simulateHistory(
            Money previousMonthSpentAmount,
            List<SpendingRecord> currentMonthRecords,
            List<SpendingRecord> currentYearRecords
    ) {
        EvaluationState state = new EvaluationState();
        Set<java.util.UUID> currentMonthRecordIds = currentMonthRecords.stream()
                .map(SpendingRecord::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<SpendingRecord> orderedCurrentYearRecords = currentYearRecords.stream()
                .sorted(Comparator.comparing(SpendingRecord::spentOn).thenComparing(SpendingRecord::id))
                .toList();

        for (SpendingRecord currentYearRecord : orderedCurrentYearRecords) {
            List<AppliedBenefit> appliedBenefits = evaluateTransaction(
                    currentYearRecord.amount(),
                    currentYearRecord.merchantName(),
                    currentYearRecord.merchantCategory(),
                    currentYearRecord.paymentTags(),
                    previousMonthSpentAmount,
                    state
            );
            state.consume(appliedBenefits, currentMonthRecordIds.contains(currentYearRecord.id()));
        }

        return state;
    }

    private List<AppliedBenefit> evaluateTransaction(
            Money amount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags,
            Money previousMonthSpentAmount,
            EvaluationState state
    ) {
        List<RuleCandidate> selectedByExclusiveGroup = selectBestRulesByExclusiveGroup(
                amount,
                merchantName,
                merchantCategory,
                paymentTags,
                previousMonthSpentAmount,
                state
        );
        if (selectedByExclusiveGroup.isEmpty()) {
            return List.of();
        }

        List<AppliedBenefit> appliedBenefits = new ArrayList<>();
        Map<String, List<RuleCandidate>> sharedGroups = new HashMap<>();

        for (RuleCandidate selectedRule : selectedByExclusiveGroup) {
            Money ruleSpecificBenefit = applyRuleSpecificLimits(selectedRule, previousMonthSpentAmount, state);
            if (ruleSpecificBenefit.amount() == 0) {
                continue;
            }

            if (selectedRule.rule().sharedLimitGroupId() == null) {
                appliedBenefits.add(new AppliedBenefit(selectedRule.rule(), ruleSpecificBenefit, selectedRule.rawBenefitAmount()));
                continue;
            }

            sharedGroups.computeIfAbsent(selectedRule.rule().sharedLimitGroupId(), ignored -> new ArrayList<>())
                    .add(selectedRule.withRuleSpecificBenefit(ruleSpecificBenefit));
        }

        for (List<RuleCandidate> sharedGroupCandidates : sharedGroups.values()) {
            appliedBenefits.addAll(applySharedGroupLimits(sharedGroupCandidates, previousMonthSpentAmount, state));
        }

        return appliedBenefits;
    }

    private List<RuleCandidate> selectBestRulesByExclusiveGroup(
            Money amount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags,
            Money previousMonthSpentAmount,
            EvaluationState state
    ) {
        Map<String, RuleCandidate> bestByExclusiveGroup = new HashMap<>();
        for (BenefitRule benefitRule : benefitRules) {
            if (!benefitRule.isEligible(
                    amount,
                    merchantName,
                    merchantCategory,
                    paymentTags,
                    previousMonthSpentAmount,
                    state.monthlyRuleCount(benefitRule),
                    state.yearlyRuleCount(benefitRule)
            )) {
                continue;
            }

            Money rawBenefitAmount = benefitRule.estimateRawBenefit(amount);
            if (rawBenefitAmount.amount() == 0) {
                continue;
            }

            RuleCandidate candidate = new RuleCandidate(benefitRule, rawBenefitAmount, Money.ZERO);
            bestByExclusiveGroup.merge(
                    benefitRule.exclusiveGroupId(),
                    candidate,
                    (left, right) -> left.rawBenefitAmount().amount() >= right.rawBenefitAmount().amount() ? left : right
            );
        }
        return List.copyOf(bestByExclusiveGroup.values());
    }

    private Money applyRuleSpecificLimits(RuleCandidate candidate, Money previousMonthSpentAmount, EvaluationState state) {
        BenefitRule benefitRule = candidate.rule();
        long appliedBenefit = candidate.rawBenefitAmount().amount();

        Money monthlyCap = benefitRule.monthlyCapFor(previousMonthSpentAmount);
        if (monthlyCap.amount() > 0) {
            long remainingMonthlyCap = Math.max(0L, monthlyCap.amount() - state.monthlyRuleBenefitUsed(benefitRule).amount());
            appliedBenefit = Math.min(appliedBenefit, remainingMonthlyCap);
        }

        if (benefitRule.yearlyBenefitCap().amount() > 0) {
            long remainingYearlyCap = Math.max(0L, benefitRule.yearlyBenefitCap().amount() - state.yearlyRuleBenefitUsed(benefitRule).amount());
            appliedBenefit = Math.min(appliedBenefit, remainingYearlyCap);
        }

        return Money.won(Math.max(0L, appliedBenefit));
    }

    private List<AppliedBenefit> applySharedGroupLimits(
            List<RuleCandidate> sharedGroupCandidates,
            Money previousMonthSpentAmount,
            EvaluationState state
    ) {
        if (sharedGroupCandidates.isEmpty()) {
            return List.of();
        }

        BenefitRule groupRule = sharedGroupCandidates.get(0).rule();
        long remainingSharedAmount = Long.MAX_VALUE;
        Money sharedMonthlyCap = groupRule.sharedMonthlyCapFor(previousMonthSpentAmount);
        if (sharedMonthlyCap.amount() > 0) {
            remainingSharedAmount = Math.min(
                    remainingSharedAmount,
                    Math.max(0L, sharedMonthlyCap.amount() - state.monthlySharedBenefitUsed(groupRule.sharedLimitGroupId()).amount())
            );
        }
        if (groupRule.sharedYearlyBenefitCap().amount() > 0) {
            remainingSharedAmount = Math.min(
                    remainingSharedAmount,
                    Math.max(0L, groupRule.sharedYearlyBenefitCap().amount() - state.yearlySharedBenefitUsed(groupRule.sharedLimitGroupId()).amount())
            );
        }

        List<RuleCandidate> orderedCandidates = sharedGroupCandidates.stream()
                .sorted(Comparator.comparingLong((RuleCandidate candidate) -> candidate.ruleSpecificBenefit().amount()).reversed())
                .toList();
        List<AppliedBenefit> appliedBenefits = new ArrayList<>();

        for (RuleCandidate orderedCandidate : orderedCandidates) {
            if (remainingSharedAmount == 0) {
                break;
            }

            long appliedAmount = orderedCandidate.ruleSpecificBenefit().amount();
            if (remainingSharedAmount != Long.MAX_VALUE) {
                appliedAmount = Math.min(appliedAmount, remainingSharedAmount);
            }
            if (appliedAmount == 0) {
                continue;
            }

            appliedBenefits.add(new AppliedBenefit(
                    orderedCandidate.rule(),
                    Money.won(appliedAmount),
                    orderedCandidate.rawBenefitAmount()
            ));
            if (remainingSharedAmount != Long.MAX_VALUE) {
                remainingSharedAmount -= appliedAmount;
            }
        }

        return appliedBenefits;
    }

    private List<PerformanceTier> sortedTiers() {
        return tiers.stream()
                .sorted(Comparator.comparingLong(tier -> tier.targetAmount().amount()))
                .toList();
    }

    private record RuleCandidate(BenefitRule rule, Money rawBenefitAmount, Money ruleSpecificBenefit) {
        private RuleCandidate withRuleSpecificBenefit(Money ruleSpecificBenefit) {
            return new RuleCandidate(rule, rawBenefitAmount, ruleSpecificBenefit);
        }
    }

    private static final class EvaluationState {
        private final Map<String, Money> monthlyRuleBenefitUsed = new HashMap<>();
        private final Map<String, Money> yearlyRuleBenefitUsed = new HashMap<>();
        private final Map<String, Integer> monthlyRuleCountUsed = new HashMap<>();
        private final Map<String, Integer> yearlyRuleCountUsed = new HashMap<>();
        private final Map<String, Money> monthlySharedBenefitUsed = new HashMap<>();
        private final Map<String, Money> yearlySharedBenefitUsed = new HashMap<>();

        private int monthlyRuleCount(BenefitRule benefitRule) {
            return monthlyRuleCountUsed.getOrDefault(benefitRule.ruleId(), 0);
        }

        private int yearlyRuleCount(BenefitRule benefitRule) {
            return yearlyRuleCountUsed.getOrDefault(benefitRule.ruleId(), 0);
        }

        private Money monthlyRuleBenefitUsed(BenefitRule benefitRule) {
            return monthlyRuleBenefitUsed.getOrDefault(benefitRule.ruleId(), Money.ZERO);
        }

        private Money yearlyRuleBenefitUsed(BenefitRule benefitRule) {
            return yearlyRuleBenefitUsed.getOrDefault(benefitRule.ruleId(), Money.ZERO);
        }

        private Money monthlySharedBenefitUsed(String sharedLimitGroupId) {
            return monthlySharedBenefitUsed.getOrDefault(sharedLimitGroupId, Money.ZERO);
        }

        private Money yearlySharedBenefitUsed(String sharedLimitGroupId) {
            return yearlySharedBenefitUsed.getOrDefault(sharedLimitGroupId, Money.ZERO);
        }

        private void consume(List<AppliedBenefit> appliedBenefits, boolean isCurrentMonthRecord) {
            Set<String> monthlyCountedRules = new HashSet<>();
            Set<String> yearlyCountedRules = new HashSet<>();

            for (AppliedBenefit appliedBenefit : appliedBenefits) {
                BenefitRule benefitRule = appliedBenefit.benefitRule();
                yearlyRuleBenefitUsed.merge(benefitRule.ruleId(), appliedBenefit.benefitAmount(), Money::add);
                yearlyCountedRules.add(benefitRule.ruleId());

                if (benefitRule.sharedLimitGroupId() != null) {
                    yearlySharedBenefitUsed.merge(benefitRule.sharedLimitGroupId(), appliedBenefit.benefitAmount(), Money::add);
                }

                if (isCurrentMonthRecord) {
                    monthlyRuleBenefitUsed.merge(benefitRule.ruleId(), appliedBenefit.benefitAmount(), Money::add);
                    monthlyCountedRules.add(benefitRule.ruleId());
                    if (benefitRule.sharedLimitGroupId() != null) {
                        monthlySharedBenefitUsed.merge(benefitRule.sharedLimitGroupId(), appliedBenefit.benefitAmount(), Money::add);
                    }
                }
            }

            for (String countedRule : yearlyCountedRules) {
                yearlyRuleCountUsed.merge(countedRule, 1, Integer::sum);
            }
            if (isCurrentMonthRecord) {
                for (String countedRule : monthlyCountedRules) {
                    monthlyRuleCountUsed.merge(countedRule, 1, Integer::sum);
                }
            }
        }
    }
}
