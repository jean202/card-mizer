package com.jean202.cardmizer.infra.persistence.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier;
import com.jean202.cardmizer.core.domain.BenefitRule;
import com.jean202.cardmizer.core.domain.BenefitType;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.out.LoadCardPerformancePoliciesPort;
import com.jean202.cardmizer.core.port.out.ReplaceCardPerformancePolicyPort;
import com.jean202.cardmizer.core.port.out.SaveCardPerformancePolicyPort;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Transactional
public class JpaCardPerformancePolicyAdapter implements
        LoadCardPerformancePoliciesPort,
        SaveCardPerformancePolicyPort,
        ReplaceCardPerformancePolicyPort {
    private final JpaCardPerformancePolicyRepository repository;
    private final ObjectMapper objectMapper;

    public JpaCardPerformancePolicyAdapter(
            JpaCardPerformancePolicyRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardPerformancePolicy> loadAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(CardPerformancePolicy cardPerformancePolicy) {
        if (repository.existsById(cardPerformancePolicy.cardId().value())) {
            throw new IllegalArgumentException("Card policy already exists: " + cardPerformancePolicy.cardId().value());
        }
        repository.save(toEntity(cardPerformancePolicy));
    }

    @Override
    public void replace(CardPerformancePolicy cardPerformancePolicy) {
        repository.save(toEntity(cardPerformancePolicy));
    }

    private JpaCardPerformancePolicyEntity toEntity(CardPerformancePolicy cardPerformancePolicy) {
        return new JpaCardPerformancePolicyEntity(
                cardPerformancePolicy.cardId().value(),
                writeJson(cardPerformancePolicy.tiers().stream().map(PerformanceTierDocument::from).toList()),
                writeJson(cardPerformancePolicy.benefitRules().stream().map(BenefitRuleDocument::from).toList())
        );
    }

    private CardPerformancePolicy toDomain(JpaCardPerformancePolicyEntity entity) {
        List<PerformanceTier> tiers = readJson(entity.getTiersJson(), new TypeReference<List<PerformanceTierDocument>>() {
        }).stream()
                .map(PerformanceTierDocument::toDomain)
                .toList();
        List<BenefitRule> benefitRules = readJson(entity.getBenefitRulesJson(), new TypeReference<List<BenefitRuleDocument>>() {
        }).stream()
                .map(BenefitRuleDocument::toDomain)
                .toList();
        return new CardPerformancePolicy(new CardId(entity.getCardId()), tiers, benefitRules);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Failed to serialize card performance policy", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Failed to deserialize card performance policy", exception);
        }
    }

    private record PerformanceTierDocument(
            String code,
            long targetAmount,
            String benefitSummary
    ) {
        private static PerformanceTierDocument from(PerformanceTier performanceTier) {
            return new PerformanceTierDocument(
                    performanceTier.code(),
                    performanceTier.targetAmount().amount(),
                    performanceTier.benefitSummary()
            );
        }

        private PerformanceTier toDomain() {
            return new PerformanceTier(code, Money.won(targetAmount), benefitSummary);
        }
    }

    private record BenefitMonthlyCapTierDocument(
            long minimumPreviousMonthSpent,
            long monthlyCap
    ) {
        private static BenefitMonthlyCapTierDocument from(BenefitMonthlyCapTier benefitMonthlyCapTier) {
            return new BenefitMonthlyCapTierDocument(
                    benefitMonthlyCapTier.minimumPreviousMonthSpent().amount(),
                    benefitMonthlyCapTier.monthlyCap().amount()
            );
        }

        private BenefitMonthlyCapTier toDomain() {
            return new BenefitMonthlyCapTier(
                    Money.won(minimumPreviousMonthSpent),
                    Money.won(monthlyCap)
            );
        }
    }

    private record BenefitRuleDocument(
            String ruleId,
            String benefitSummary,
            BenefitType benefitType,
            Set<String> merchantCategories,
            Set<String> merchantKeywords,
            Set<String> requiredTags,
            Set<String> excludedTags,
            String exclusiveGroupId,
            String sharedLimitGroupId,
            Integer rateBasisPoints,
            Long fixedBenefitAmount,
            Long minimumPaymentAmount,
            Long perTransactionCap,
            Long minimumPreviousMonthSpent,
            List<BenefitMonthlyCapTierDocument> monthlyCapTiers,
            Long yearlyBenefitCap,
            Integer monthlyCountLimit,
            Integer yearlyCountLimit,
            List<BenefitMonthlyCapTierDocument> sharedMonthlyCapTiers,
            Long sharedYearlyBenefitCap
    ) {
        private static BenefitRuleDocument from(BenefitRule benefitRule) {
            return new BenefitRuleDocument(
                    benefitRule.ruleId(),
                    benefitRule.benefitSummary(),
                    benefitRule.benefitType(),
                    benefitRule.merchantCategories(),
                    benefitRule.merchantKeywords(),
                    benefitRule.requiredTags(),
                    benefitRule.excludedTags(),
                    benefitRule.exclusiveGroupId(),
                    benefitRule.sharedLimitGroupId(),
                    benefitRule.benefitType() == BenefitType.RATE_PERCENT ? benefitRule.rateBasisPoints() : null,
                    benefitRule.benefitType() == BenefitType.FIXED_AMOUNT ? benefitRule.fixedBenefitAmount().amount() : null,
                    benefitRule.minimumPaymentAmount().amount(),
                    benefitRule.perTransactionCap().amount(),
                    benefitRule.minimumPreviousMonthSpent().amount(),
                    benefitRule.monthlyCapTiers().stream().map(BenefitMonthlyCapTierDocument::from).toList(),
                    benefitRule.yearlyBenefitCap().amount(),
                    benefitRule.monthlyCountLimit(),
                    benefitRule.yearlyCountLimit(),
                    benefitRule.sharedMonthlyCapTiers().stream().map(BenefitMonthlyCapTierDocument::from).toList(),
                    benefitRule.sharedYearlyBenefitCap().amount()
            );
        }

        private BenefitRule toDomain() {
            BenefitRule.Builder builder = switch (benefitType) {
                case RATE_PERCENT -> BenefitRule.percentage(ruleId, benefitSummary, 1)
                        .rateBasisPoints(rateBasisPoints == null ? 0 : rateBasisPoints);
                case FIXED_AMOUNT -> BenefitRule.fixedAmount(
                        ruleId,
                        benefitSummary,
                        Money.won(fixedBenefitAmount == null ? 0L : fixedBenefitAmount)
                );
            };

            builder.categories(toArray(merchantCategories));
            builder.merchantKeywords(toArray(merchantKeywords));
            builder.requiredTags(toArray(requiredTags));
            builder.excludedTags(toArray(excludedTags));
            builder.exclusiveGroup(exclusiveGroupId);
            builder.sharedLimitGroup(sharedLimitGroupId);
            builder.minimumPaymentAmount(Money.won(defaultLong(minimumPaymentAmount)));
            builder.perTransactionCap(Money.won(defaultLong(perTransactionCap)));
            builder.minimumPreviousMonthSpent(Money.won(defaultLong(minimumPreviousMonthSpent)));
            builder.yearlyBenefitCap(Money.won(defaultLong(yearlyBenefitCap)));
            builder.monthlyCountLimit(defaultInt(monthlyCountLimit));
            builder.yearlyCountLimit(defaultInt(yearlyCountLimit));
            builder.sharedYearlyBenefitCap(Money.won(defaultLong(sharedYearlyBenefitCap)));

            if (monthlyCapTiers != null && !monthlyCapTiers.isEmpty()) {
                builder.monthlyCapTiers(monthlyCapTiers.stream()
                        .map(BenefitMonthlyCapTierDocument::toDomain)
                        .toArray(BenefitMonthlyCapTier[]::new));
            }
            if (sharedMonthlyCapTiers != null && !sharedMonthlyCapTiers.isEmpty()) {
                builder.sharedMonthlyCapTiers(sharedMonthlyCapTiers.stream()
                        .map(BenefitMonthlyCapTierDocument::toDomain)
                        .toArray(BenefitMonthlyCapTier[]::new));
            }
            return builder.build();
        }

        private String[] toArray(Set<String> values) {
            return values == null ? new String[0] : values.toArray(String[]::new);
        }

        private long defaultLong(Long value) {
            return value == null ? 0L : value;
        }

        private int defaultInt(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
