package com.jean202.cardmizer.api.cards;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.BenefitMonthlyCapTier;
import com.jean202.cardmizer.core.domain.BenefitRule;
import com.jean202.cardmizer.core.domain.BenefitType;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardPerformancePolicy;
import com.jean202.cardmizer.core.domain.PerformanceTier;
import com.jean202.cardmizer.core.port.in.GetCardPerformancePolicyUseCase;
import com.jean202.cardmizer.core.port.in.ReplaceCardPerformancePolicyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/performance-policy")
public class CardPerformancePolicyController {
    private final GetCardPerformancePolicyUseCase getCardPerformancePolicyUseCase;
    private final ReplaceCardPerformancePolicyUseCase replaceCardPerformancePolicyUseCase;

    public CardPerformancePolicyController(
            GetCardPerformancePolicyUseCase getCardPerformancePolicyUseCase,
            ReplaceCardPerformancePolicyUseCase replaceCardPerformancePolicyUseCase
    ) {
        this.getCardPerformancePolicyUseCase = getCardPerformancePolicyUseCase;
        this.replaceCardPerformancePolicyUseCase = replaceCardPerformancePolicyUseCase;
    }

    @GetMapping
    public CardPerformancePolicyResponse get(@PathVariable String cardId) {
        return CardPerformancePolicyResponse.from(
                getCardPerformancePolicyUseCase.get(new CardId(cardId))
        );
    }

    @PutMapping
    public CardPerformancePolicyResponse replace(
            @PathVariable String cardId,
            @Valid @RequestBody ReplaceCardPerformancePolicyRequest request
    ) {
        CardPerformancePolicy cardPerformancePolicy = request.toDomain(new CardId(cardId));
        replaceCardPerformancePolicyUseCase.replace(cardPerformancePolicy);
        return CardPerformancePolicyResponse.from(cardPerformancePolicy);
    }

    @PatchMapping
    public CardPerformancePolicyResponse patch(
            @PathVariable String cardId,
            @Valid @RequestBody PatchCardPerformancePolicyRequest request
    ) {
        CardId domainCardId = new CardId(cardId);
        CardPerformancePolicy mergedPolicy = request.merge(
                domainCardId,
                getCardPerformancePolicyUseCase.get(domainCardId)
        );
        replaceCardPerformancePolicyUseCase.replace(mergedPolicy);
        return CardPerformancePolicyResponse.from(mergedPolicy);
    }

    public record ReplaceCardPerformancePolicyRequest(
            @NotEmpty(message = "tiers must not be empty")
            List<@Valid PerformanceTierRequest> tiers,
            List<@Valid BenefitRuleRequest> benefitRules
    ) {
        CardPerformancePolicy toDomain(CardId cardId) {
            if (tiers == null) {
                throw new IllegalArgumentException("tiers must not be null");
            }
            return new CardPerformancePolicy(cardId, toDomainTiers(tiers), toDomainBenefitRules(benefitRules));
        }
    }

    public record PatchCardPerformancePolicyRequest(
            List<@Valid PerformanceTierRequest> tiers,
            List<@Valid BenefitRuleRequest> benefitRules
    ) {
        CardPerformancePolicy merge(CardId cardId, CardPerformancePolicy currentPolicy) {
            if (tiers == null && benefitRules == null) {
                throw new IllegalArgumentException("At least one of tiers or benefitRules must be provided");
            }

            List<PerformanceTier> mergedTiers = tiers == null
                    ? currentPolicy.tiers()
                    : toDomainTiers(tiers);
            List<BenefitRule> mergedBenefitRules = benefitRules == null
                    ? currentPolicy.benefitRules()
                    : toDomainBenefitRules(benefitRules);
            return new CardPerformancePolicy(cardId, mergedTiers, mergedBenefitRules);
        }
    }

    public record PerformanceTierRequest(
            @NotBlank(message = "code must not be blank")
            String code,
            @PositiveOrZero(message = "targetAmount must be zero or positive")
            long targetAmount,
            @NotBlank(message = "benefitSummary must not be blank")
            String benefitSummary
    ) {
        PerformanceTier toDomain() {
            return new PerformanceTier(code, Money.won(targetAmount), benefitSummary);
        }
    }

    public record BenefitRuleRequest(
            @NotBlank(message = "ruleId must not be blank")
            String ruleId,
            @NotBlank(message = "benefitSummary must not be blank")
            String benefitSummary,
            @NotBlank(message = "benefitType must not be blank")
            @Pattern(
                    regexp = "(?i)^(RATE_PERCENT|FIXED_AMOUNT)$",
                    message = "benefitType must be RATE_PERCENT or FIXED_AMOUNT"
            )
            String benefitType,
            Set<String> merchantCategories,
            Set<String> merchantKeywords,
            Set<String> requiredTags,
            Set<String> excludedTags,
            String exclusiveGroupId,
            String sharedLimitGroupId,
            @PositiveOrZero(message = "rateBasisPoints must be zero or positive")
            Integer rateBasisPoints,
            @PositiveOrZero(message = "fixedBenefitAmount must be zero or positive")
            Long fixedBenefitAmount,
            @PositiveOrZero(message = "minimumPaymentAmount must be zero or positive")
            Long minimumPaymentAmount,
            @PositiveOrZero(message = "perTransactionCap must be zero or positive")
            Long perTransactionCap,
            @PositiveOrZero(message = "minimumPreviousMonthSpent must be zero or positive")
            Long minimumPreviousMonthSpent,
            List<@Valid BenefitMonthlyCapTierRequest> monthlyCapTiers,
            @PositiveOrZero(message = "yearlyBenefitCap must be zero or positive")
            Long yearlyBenefitCap,
            @PositiveOrZero(message = "monthlyCountLimit must be zero or positive")
            Integer monthlyCountLimit,
            @PositiveOrZero(message = "yearlyCountLimit must be zero or positive")
            Integer yearlyCountLimit,
            List<@Valid BenefitMonthlyCapTierRequest> sharedMonthlyCapTiers,
            @PositiveOrZero(message = "sharedYearlyBenefitCap must be zero or positive")
            Long sharedYearlyBenefitCap
    ) {
        BenefitRule toDomain() {
            BenefitType parsedBenefitType = parseBenefitType(benefitType);
            BenefitRule.Builder builder = switch (parsedBenefitType) {
                case RATE_PERCENT -> BenefitRule.percentage(ruleId, benefitSummary, 1)
                        .rateBasisPoints(requiredRateBasisPoints());
                case FIXED_AMOUNT -> BenefitRule.fixedAmount(
                        ruleId,
                        benefitSummary,
                        Money.won(requiredFixedBenefitAmount())
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
                        .map(BenefitMonthlyCapTierRequest::toDomain)
                        .toArray(BenefitMonthlyCapTier[]::new));
            }
            if (sharedMonthlyCapTiers != null && !sharedMonthlyCapTiers.isEmpty()) {
                builder.sharedMonthlyCapTiers(sharedMonthlyCapTiers.stream()
                        .map(BenefitMonthlyCapTierRequest::toDomain)
                        .toArray(BenefitMonthlyCapTier[]::new));
            }

            return builder.build();
        }

        private BenefitType parseBenefitType(String rawBenefitType) {
            if (rawBenefitType == null || rawBenefitType.isBlank()) {
                throw new IllegalArgumentException("benefitType must not be blank");
            }

            try {
                return BenefitType.valueOf(rawBenefitType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unsupported benefitType: " + rawBenefitType, exception);
            }
        }

        private int requiredRateBasisPoints() {
            if (rateBasisPoints == null) {
                throw new IllegalArgumentException("rateBasisPoints must not be null for RATE_PERCENT");
            }
            if (fixedBenefitAmount != null) {
                throw new IllegalArgumentException("fixedBenefitAmount must be null for RATE_PERCENT");
            }
            return rateBasisPoints;
        }

        private long requiredFixedBenefitAmount() {
            if (fixedBenefitAmount == null) {
                throw new IllegalArgumentException("fixedBenefitAmount must not be null for FIXED_AMOUNT");
            }
            if (rateBasisPoints != null) {
                throw new IllegalArgumentException("rateBasisPoints must be null for FIXED_AMOUNT");
            }
            return fixedBenefitAmount;
        }

        private String[] toArray(Set<String> values) {
            return values == null ? new String[0] : values.stream().toArray(String[]::new);
        }

        private long defaultLong(Long value) {
            return value == null ? 0L : value;
        }

        private int defaultInt(Integer value) {
            return value == null ? 0 : value;
        }
    }

    public record BenefitMonthlyCapTierRequest(
            @PositiveOrZero(message = "minimumPreviousMonthSpent must be zero or positive")
            long minimumPreviousMonthSpent,
            @PositiveOrZero(message = "monthlyCap must be zero or positive")
            long monthlyCap
    ) {
        BenefitMonthlyCapTier toDomain() {
            return new BenefitMonthlyCapTier(
                    Money.won(minimumPreviousMonthSpent),
                    Money.won(monthlyCap)
            );
        }
    }

    public record CardPerformancePolicyResponse(
            String cardId,
            List<PerformanceTierResponse> tiers,
            List<BenefitRuleResponse> benefitRules
    ) {
        static CardPerformancePolicyResponse from(CardPerformancePolicy cardPerformancePolicy) {
            return new CardPerformancePolicyResponse(
                    cardPerformancePolicy.cardId().value(),
                    cardPerformancePolicy.tiers().stream()
                            .map(PerformanceTierResponse::from)
                            .toList(),
                    cardPerformancePolicy.benefitRules().stream()
                            .map(BenefitRuleResponse::from)
                            .toList()
            );
        }
    }

    public record PerformanceTierResponse(
            String code,
            long targetAmount,
            String benefitSummary
    ) {
        static PerformanceTierResponse from(PerformanceTier performanceTier) {
            return new PerformanceTierResponse(
                    performanceTier.code(),
                    performanceTier.targetAmount().amount(),
                    performanceTier.benefitSummary()
            );
        }
    }

    public record BenefitRuleResponse(
            String ruleId,
            String benefitSummary,
            String benefitType,
            List<String> merchantCategories,
            List<String> merchantKeywords,
            List<String> requiredTags,
            List<String> excludedTags,
            String exclusiveGroupId,
            String sharedLimitGroupId,
            Integer rateBasisPoints,
            Long fixedBenefitAmount,
            long minimumPaymentAmount,
            long perTransactionCap,
            long minimumPreviousMonthSpent,
            List<BenefitMonthlyCapTierResponse> monthlyCapTiers,
            long yearlyBenefitCap,
            int monthlyCountLimit,
            int yearlyCountLimit,
            List<BenefitMonthlyCapTierResponse> sharedMonthlyCapTiers,
            long sharedYearlyBenefitCap
    ) {
        static BenefitRuleResponse from(BenefitRule benefitRule) {
            return new BenefitRuleResponse(
                    benefitRule.ruleId(),
                    benefitRule.benefitSummary(),
                    benefitRule.benefitType().name(),
                    benefitRule.merchantCategories().stream().sorted().toList(),
                    benefitRule.merchantKeywords().stream().sorted().toList(),
                    benefitRule.requiredTags().stream().sorted().toList(),
                    benefitRule.excludedTags().stream().sorted().toList(),
                    benefitRule.exclusiveGroupId(),
                    benefitRule.sharedLimitGroupId(),
                    benefitRule.benefitType() == BenefitType.RATE_PERCENT ? benefitRule.rateBasisPoints() : null,
                    benefitRule.benefitType() == BenefitType.FIXED_AMOUNT ? benefitRule.fixedBenefitAmount().amount() : null,
                    benefitRule.minimumPaymentAmount().amount(),
                    benefitRule.perTransactionCap().amount(),
                    benefitRule.minimumPreviousMonthSpent().amount(),
                    benefitRule.monthlyCapTiers().stream()
                            .map(BenefitMonthlyCapTierResponse::from)
                            .toList(),
                    benefitRule.yearlyBenefitCap().amount(),
                    benefitRule.monthlyCountLimit(),
                    benefitRule.yearlyCountLimit(),
                    benefitRule.sharedMonthlyCapTiers().stream()
                            .map(BenefitMonthlyCapTierResponse::from)
                            .toList(),
                    benefitRule.sharedYearlyBenefitCap().amount()
            );
        }
    }

    public record BenefitMonthlyCapTierResponse(
            long minimumPreviousMonthSpent,
            long monthlyCap
    ) {
        static BenefitMonthlyCapTierResponse from(BenefitMonthlyCapTier benefitMonthlyCapTier) {
            return new BenefitMonthlyCapTierResponse(
                    benefitMonthlyCapTier.minimumPreviousMonthSpent().amount(),
                    benefitMonthlyCapTier.monthlyCap().amount()
            );
        }
    }

    private static List<PerformanceTier> toDomainTiers(List<PerformanceTierRequest> tiers) {
        return tiers.stream()
                .map(PerformanceTierRequest::toDomain)
                .toList();
    }

    private static List<BenefitRule> toDomainBenefitRules(List<BenefitRuleRequest> benefitRules) {
        return benefitRules == null
                ? List.of()
                : benefitRules.stream().map(BenefitRuleRequest::toDomain).toList();
    }
}
