package com.jean202.cardmizer.core.domain;

import com.jean202.cardmizer.common.Money;
import java.util.Objects;

public record AppliedBenefit(BenefitRule benefitRule, Money benefitAmount, Money rawBenefitAmount) {
    public AppliedBenefit {
        benefitRule = Objects.requireNonNull(benefitRule, "benefitRule must not be null");
        benefitAmount = Objects.requireNonNull(benefitAmount, "benefitAmount must not be null");
        rawBenefitAmount = Objects.requireNonNull(rawBenefitAmount, "rawBenefitAmount must not be null");
    }

    public boolean wasCapped() {
        return rawBenefitAmount.amount() > benefitAmount.amount();
    }
}
