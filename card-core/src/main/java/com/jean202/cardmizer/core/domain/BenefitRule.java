package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class BenefitRule {
    private static final String ANY_CATEGORY = "ANY";

    private final String ruleId;
    private final Set<String> merchantCategories;
    private final Set<String> merchantKeywords;
    private final Set<String> requiredTags;
    private final Set<String> excludedTags;
    private final String exclusiveGroupId;
    private final String sharedLimitGroupId;
    private final BenefitType benefitType;
    private final int rateBasisPoints;
    private final Money fixedBenefitAmount;
    private final Money minimumPaymentAmount;
    private final Money perTransactionCap;
    private final Money minimumPreviousMonthSpent;
    private final List<BenefitMonthlyCapTier> monthlyCapTiers;
    private final Money yearlyBenefitCap;
    private final int monthlyCountLimit;
    private final int yearlyCountLimit;
    private final List<BenefitMonthlyCapTier> sharedMonthlyCapTiers;
    private final Money sharedYearlyBenefitCap;
    private final String benefitSummary;

    private BenefitRule(Builder builder) {
        this.ruleId = requireText(builder.ruleId, "ruleId");
        this.merchantCategories = normalizeCategories(builder.merchantCategories);
        this.merchantKeywords = normalizeTokens(builder.merchantKeywords);
        this.requiredTags = normalizeTokens(builder.requiredTags);
        this.excludedTags = normalizeTokens(builder.excludedTags);
        this.exclusiveGroupId = normalizeIdentifier(builder.exclusiveGroupId == null ? builder.ruleId : builder.exclusiveGroupId);
        this.sharedLimitGroupId = normalizeNullable(builder.sharedLimitGroupId);
        this.benefitType = Objects.requireNonNull(builder.benefitType, "benefitType must not be null");
        this.rateBasisPoints = builder.rateBasisPoints;
        this.fixedBenefitAmount = Objects.requireNonNull(builder.fixedBenefitAmount, "fixedBenefitAmount must not be null");
        this.minimumPaymentAmount = Objects.requireNonNull(
                builder.minimumPaymentAmount,
                "minimumPaymentAmount must not be null"
        );
        this.perTransactionCap = Objects.requireNonNull(builder.perTransactionCap, "perTransactionCap must not be null");
        this.minimumPreviousMonthSpent = Objects.requireNonNull(
                builder.minimumPreviousMonthSpent,
                "minimumPreviousMonthSpent must not be null"
        );
        this.monthlyCapTiers = normalizeTiers(builder.monthlyCapTiers);
        this.yearlyBenefitCap = Objects.requireNonNull(builder.yearlyBenefitCap, "yearlyBenefitCap must not be null");
        this.monthlyCountLimit = builder.monthlyCountLimit;
        this.yearlyCountLimit = builder.yearlyCountLimit;
        this.sharedMonthlyCapTiers = normalizeTiers(builder.sharedMonthlyCapTiers);
        this.sharedYearlyBenefitCap = Objects.requireNonNull(
                builder.sharedYearlyBenefitCap,
                "sharedYearlyBenefitCap must not be null"
        );
        this.benefitSummary = requireText(builder.benefitSummary, "benefitSummary");

        if (minimumPaymentAmount.amount() < 0 || perTransactionCap.amount() < 0 || minimumPreviousMonthSpent.amount() < 0) {
            throw new IllegalArgumentException("Benefit thresholds must not be negative");
        }
        if (yearlyBenefitCap.amount() < 0 || sharedYearlyBenefitCap.amount() < 0) {
            throw new IllegalArgumentException("Benefit caps must not be negative");
        }
        if (monthlyCountLimit < 0 || yearlyCountLimit < 0) {
            throw new IllegalArgumentException("Benefit counts must not be negative");
        }

        validateBenefitValue();
    }

    public static Builder percentage(String ruleId, String benefitSummary, int ratePercent) {
        return new Builder(ruleId, benefitSummary, BenefitType.RATE_PERCENT)
                .ratePercent(ratePercent);
    }

    public static Builder fixedAmount(String ruleId, String benefitSummary, Money fixedBenefitAmount) {
        return new Builder(ruleId, benefitSummary, BenefitType.FIXED_AMOUNT)
                .fixedBenefitAmount(fixedBenefitAmount);
    }

    public String ruleId() {
        return ruleId;
    }

    public String benefitSummary() {
        return benefitSummary;
    }

    public Set<String> merchantCategories() {
        return merchantCategories;
    }

    public Set<String> merchantKeywords() {
        return merchantKeywords;
    }

    public Set<String> requiredTags() {
        return requiredTags;
    }

    public Set<String> excludedTags() {
        return excludedTags;
    }

    public String exclusiveGroupId() {
        return exclusiveGroupId;
    }

    public String sharedLimitGroupId() {
        return sharedLimitGroupId;
    }

    public BenefitType benefitType() {
        return benefitType;
    }

    public int rateBasisPoints() {
        return rateBasisPoints;
    }

    public Money fixedBenefitAmount() {
        return fixedBenefitAmount;
    }

    public Money minimumPaymentAmount() {
        return minimumPaymentAmount;
    }

    public Money perTransactionCap() {
        return perTransactionCap;
    }

    public Money minimumPreviousMonthSpent() {
        return minimumPreviousMonthSpent;
    }

    public boolean matches(String merchantName, String merchantCategory, Set<String> paymentTags) {
        return matchesCategory(merchantCategory)
                && matchesMerchant(merchantName)
                && matchesTags(paymentTags);
    }

    public boolean isEligible(
            Money paymentAmount,
            String merchantName,
            String merchantCategory,
            Set<String> paymentTags,
            Money previousMonthSpent,
            int usedMonthlyCount,
            int usedYearlyCount
    ) {
        Objects.requireNonNull(paymentAmount, "paymentAmount must not be null");
        Objects.requireNonNull(previousMonthSpent, "previousMonthSpent must not be null");
        return matches(merchantName, merchantCategory, paymentTags)
                && paymentAmount.isGreaterThanOrEqual(minimumPaymentAmount)
                && previousMonthSpent.isGreaterThanOrEqual(minimumPreviousMonthSpent)
                && (monthlyCountLimit == 0 || usedMonthlyCount < monthlyCountLimit)
                && (yearlyCountLimit == 0 || usedYearlyCount < yearlyCountLimit);
    }

    public Money estimateRawBenefit(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        Money rawBenefit = switch (benefitType) {
            case RATE_PERCENT -> Money.won(amount.amount() * rateBasisPoints / 10_000L);
            case FIXED_AMOUNT -> fixedBenefitAmount;
        };
        if (perTransactionCap.amount() == 0) {
            return rawBenefit;
        }
        return Money.won(Math.min(rawBenefit.amount(), perTransactionCap.amount()));
    }

    public Money monthlyCapFor(Money previousMonthSpent) {
        return capFor(previousMonthSpent, monthlyCapTiers);
    }

    public Money sharedMonthlyCapFor(Money previousMonthSpent) {
        return capFor(previousMonthSpent, sharedMonthlyCapTiers);
    }

    public List<BenefitMonthlyCapTier> monthlyCapTiers() {
        return monthlyCapTiers;
    }

    public Money yearlyBenefitCap() {
        return yearlyBenefitCap;
    }

    public int monthlyCountLimit() {
        return monthlyCountLimit;
    }

    public int yearlyCountLimit() {
        return yearlyCountLimit;
    }

    public List<BenefitMonthlyCapTier> sharedMonthlyCapTiers() {
        return sharedMonthlyCapTiers;
    }

    public Money sharedYearlyBenefitCap() {
        return sharedYearlyBenefitCap;
    }

    private boolean matchesCategory(String category) {
        Objects.requireNonNull(category, "category must not be null");
        String normalizedCategory = normalizeIdentifier(category);
        return merchantCategories.contains(ANY_CATEGORY) || merchantCategories.contains(normalizedCategory);
    }

    private boolean matchesMerchant(String merchantName) {
        if (merchantKeywords.isEmpty()) {
            return true;
        }
        Objects.requireNonNull(merchantName, "merchantName must not be null");
        String normalizedMerchantName = merchantName.trim().toUpperCase(Locale.ROOT);
        return merchantKeywords.stream().anyMatch(normalizedMerchantName::contains);
    }

    private boolean matchesTags(Set<String> paymentTags) {
        Set<String> normalizedTags = normalizeTokens(paymentTags);
        return normalizedTags.containsAll(requiredTags)
                && excludedTags.stream().noneMatch(normalizedTags::contains);
    }

    private Money capFor(Money previousMonthSpent, List<BenefitMonthlyCapTier> capTiers) {
        Objects.requireNonNull(previousMonthSpent, "previousMonthSpent must not be null");
        if (capTiers.isEmpty()) {
            return Money.ZERO;
        }
        return capTiers.stream()
                .filter(capTier -> previousMonthSpent.isGreaterThanOrEqual(capTier.minimumPreviousMonthSpent()))
                .max(Comparator.comparingLong(capTier -> capTier.minimumPreviousMonthSpent().amount()))
                .map(BenefitMonthlyCapTier::monthlyCap)
                .orElse(Money.ZERO);
    }

    private void validateBenefitValue() {
        switch (benefitType) {
            case RATE_PERCENT -> {
                if (rateBasisPoints < 1 || rateBasisPoints > 10_000) {
                    throw new IllegalArgumentException("rateBasisPoints must be between 1 and 10000");
                }
                if (fixedBenefitAmount.amount() != 0) {
                    throw new IllegalArgumentException("fixedBenefitAmount must be zero for rate benefits");
                }
            }
            case FIXED_AMOUNT -> {
                if (rateBasisPoints != 0) {
                    throw new IllegalArgumentException("rateBasisPoints must be zero for fixed benefits");
                }
                if (fixedBenefitAmount.amount() <= 0) {
                    throw new IllegalArgumentException("fixedBenefitAmount must be positive for fixed benefits");
                }
            }
        }
    }

    private static List<BenefitMonthlyCapTier> normalizeTiers(List<BenefitMonthlyCapTier> capTiers) {
        return List.copyOf(Objects.requireNonNull(capTiers, "capTiers must not be null"));
    }

    private static Set<String> normalizeCategories(Set<String> categories) {
        Set<String> normalizedCategories = normalizeTokens(categories);
        if (normalizedCategories.isEmpty()) {
            return Set.of(ANY_CATEGORY);
        }
        return normalizedCategories;
    }

    private static Set<String> normalizeTokens(Set<String> rawValues) {
        Objects.requireNonNull(rawValues, "rawValues must not be null");
        return rawValues.stream()
                .map(BenefitRule::normalizeIdentifier)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalizeIdentifier(String value) {
        return requireText(value, "value").trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeIdentifier(value);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public static final class Builder {
        private final String ruleId;
        private final String benefitSummary;
        private final BenefitType benefitType;
        private Set<String> merchantCategories = Set.of();
        private Set<String> merchantKeywords = Set.of();
        private Set<String> requiredTags = Set.of();
        private Set<String> excludedTags = Set.of();
        private String exclusiveGroupId;
        private String sharedLimitGroupId;
        private int rateBasisPoints;
        private Money fixedBenefitAmount = Money.ZERO;
        private Money minimumPaymentAmount = Money.ZERO;
        private Money perTransactionCap = Money.ZERO;
        private Money minimumPreviousMonthSpent = Money.ZERO;
        private List<BenefitMonthlyCapTier> monthlyCapTiers = List.of();
        private Money yearlyBenefitCap = Money.ZERO;
        private int monthlyCountLimit;
        private int yearlyCountLimit;
        private List<BenefitMonthlyCapTier> sharedMonthlyCapTiers = List.of();
        private Money sharedYearlyBenefitCap = Money.ZERO;

        private Builder(String ruleId, String benefitSummary, BenefitType benefitType) {
            this.ruleId = ruleId;
            this.benefitSummary = benefitSummary;
            this.benefitType = benefitType;
        }

        public Builder categories(String... merchantCategories) {
            this.merchantCategories = normalizeVarArgs(merchantCategories);
            return this;
        }

        public Builder merchantKeywords(String... merchantKeywords) {
            this.merchantKeywords = normalizeVarArgs(merchantKeywords);
            return this;
        }

        public Builder requiredTags(String... requiredTags) {
            this.requiredTags = normalizeVarArgs(requiredTags);
            return this;
        }

        public Builder excludedTags(String... excludedTags) {
            this.excludedTags = normalizeVarArgs(excludedTags);
            return this;
        }

        public Builder exclusiveGroup(String exclusiveGroupId) {
            this.exclusiveGroupId = exclusiveGroupId;
            return this;
        }

        public Builder sharedLimitGroup(String sharedLimitGroupId) {
            this.sharedLimitGroupId = sharedLimitGroupId;
            return this;
        }

        public Builder ratePercent(int ratePercent) {
            this.rateBasisPoints = ratePercent * 100;
            return this;
        }

        public Builder rateBasisPoints(int rateBasisPoints) {
            this.rateBasisPoints = rateBasisPoints;
            return this;
        }

        public Builder fixedBenefitAmount(Money fixedBenefitAmount) {
            this.fixedBenefitAmount = fixedBenefitAmount;
            return this;
        }

        public Builder minimumPaymentAmount(Money minimumPaymentAmount) {
            this.minimumPaymentAmount = minimumPaymentAmount;
            return this;
        }

        public Builder perTransactionCap(Money perTransactionCap) {
            this.perTransactionCap = perTransactionCap;
            return this;
        }

        public Builder minimumPreviousMonthSpent(Money minimumPreviousMonthSpent) {
            this.minimumPreviousMonthSpent = minimumPreviousMonthSpent;
            return this;
        }

        public Builder monthlyCap(Money monthlyCap) {
            this.monthlyCapTiers = monthlyCap.amount() == 0
                    ? List.of()
                    : List.of(new BenefitMonthlyCapTier(Money.ZERO, monthlyCap));
            return this;
        }

        public Builder monthlyCapTiers(BenefitMonthlyCapTier... monthlyCapTiers) {
            this.monthlyCapTiers = List.copyOf(Arrays.asList(monthlyCapTiers));
            return this;
        }

        public Builder yearlyBenefitCap(Money yearlyBenefitCap) {
            this.yearlyBenefitCap = yearlyBenefitCap;
            return this;
        }

        public Builder monthlyCountLimit(int monthlyCountLimit) {
            this.monthlyCountLimit = monthlyCountLimit;
            return this;
        }

        public Builder yearlyCountLimit(int yearlyCountLimit) {
            this.yearlyCountLimit = yearlyCountLimit;
            return this;
        }

        public Builder sharedMonthlyCap(Money sharedMonthlyCap) {
            this.sharedMonthlyCapTiers = sharedMonthlyCap.amount() == 0
                    ? List.of()
                    : List.of(new BenefitMonthlyCapTier(Money.ZERO, sharedMonthlyCap));
            return this;
        }

        public Builder sharedMonthlyCapTiers(BenefitMonthlyCapTier... sharedMonthlyCapTiers) {
            this.sharedMonthlyCapTiers = List.copyOf(Arrays.asList(sharedMonthlyCapTiers));
            return this;
        }

        public Builder sharedYearlyBenefitCap(Money sharedYearlyBenefitCap) {
            this.sharedYearlyBenefitCap = sharedYearlyBenefitCap;
            return this;
        }

        public BenefitRule build() {
            return new BenefitRule(this);
        }

        private static Set<String> normalizeVarArgs(String... values) {
            if (values == null || values.length == 0) {
                return Set.of();
            }
            Set<String> normalizedValues = new LinkedHashSet<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalizedValues.add(value);
                }
            }
            return Set.copyOf(normalizedValues);
        }
    }
}
