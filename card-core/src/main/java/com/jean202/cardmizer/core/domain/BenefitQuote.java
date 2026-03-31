package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record BenefitQuote(
        List<AppliedBenefit> appliedBenefits,
        Money benefitAmount,
        Money rawBenefitAmount
) {
    public BenefitQuote {
        appliedBenefits = List.copyOf(Objects.requireNonNull(appliedBenefits, "appliedBenefits must not be null"));
        if (appliedBenefits.isEmpty()) {
            throw new IllegalArgumentException("appliedBenefits must not be empty");
        }
        Objects.requireNonNull(benefitAmount, "benefitAmount must not be null");
        Objects.requireNonNull(rawBenefitAmount, "rawBenefitAmount must not be null");
    }

    public boolean wasCapped() {
        return rawBenefitAmount.amount() > benefitAmount.amount();
    }

    public String summary() {
        return appliedBenefits.stream()
                .map(appliedBenefit -> appliedBenefit.benefitRule().benefitSummary())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        summaries -> String.join(", ", summaries)
                ));
    }
}
