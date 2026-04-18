package com.jean202.cardmizer.api.cards

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.util.Locale
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BenefitRuleRequestValidator::class])
annotation class ValidBenefitRuleRequest(
    val message: String = "Invalid benefit rule request",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PatchCardPerformancePolicyRequestValidator::class])
annotation class ValidPatchCardPerformancePolicyRequest(
    val message: String = "At least one of tiers or benefitRules must be provided",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class BenefitRuleRequestValidator :
    ConstraintValidator<ValidBenefitRuleRequest, CardPerformancePolicyController.BenefitRuleRequest> {

    override fun isValid(
        value: CardPerformancePolicyController.BenefitRuleRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) return true

        return when (value.benefitType.trim().uppercase(Locale.ROOT)) {
            "RATE_PERCENT" -> validateRatePercentRule(value, context)
            "FIXED_AMOUNT" -> validateFixedAmountRule(value, context)
            else -> true
        }
    }

    private fun validateRatePercentRule(
        value: CardPerformancePolicyController.BenefitRuleRequest,
        context: ConstraintValidatorContext,
    ): Boolean {
        var valid = true
        if (value.rateBasisPoints == null) {
            valid = false
            addViolation(context, "rateBasisPoints", "rateBasisPoints is required for RATE_PERCENT")
        } else if (value.rateBasisPoints == 0 || value.rateBasisPoints > 10_000) {
            valid = false
            addViolation(context, "rateBasisPoints", "rateBasisPoints must be between 1 and 10000 for RATE_PERCENT")
        }
        if (value.fixedBenefitAmount != null && value.fixedBenefitAmount != 0L) {
            valid = false
            addViolation(context, "fixedBenefitAmount", "fixedBenefitAmount must be omitted or zero for RATE_PERCENT")
        }
        return valid
    }

    private fun validateFixedAmountRule(
        value: CardPerformancePolicyController.BenefitRuleRequest,
        context: ConstraintValidatorContext,
    ): Boolean {
        var valid = true
        if (value.fixedBenefitAmount == null) {
            valid = false
            addViolation(context, "fixedBenefitAmount", "fixedBenefitAmount is required for FIXED_AMOUNT")
        } else if (value.fixedBenefitAmount == 0L) {
            valid = false
            addViolation(context, "fixedBenefitAmount", "fixedBenefitAmount must be greater than 0 for FIXED_AMOUNT")
        }
        if (value.rateBasisPoints != null && value.rateBasisPoints != 0) {
            valid = false
            addViolation(context, "rateBasisPoints", "rateBasisPoints must be omitted or zero for FIXED_AMOUNT")
        }
        return valid
    }

    private fun addViolation(
        context: ConstraintValidatorContext,
        field: String,
        message: String,
    ) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(field)
            .addConstraintViolation()
    }
}

class PatchCardPerformancePolicyRequestValidator :
    ConstraintValidator<ValidPatchCardPerformancePolicyRequest, CardPerformancePolicyController.PatchCardPerformancePolicyRequest> {

    override fun isValid(
        value: CardPerformancePolicyController.PatchCardPerformancePolicyRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null || value.tiers != null || value.benefitRules != null) return true

        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate("At least one of tiers or benefitRules must be provided")
            .addPropertyNode("tiers")
            .addConstraintViolation()
        context.buildConstraintViolationWithTemplate("At least one of tiers or benefitRules must be provided")
            .addPropertyNode("benefitRules")
            .addConstraintViolation()
        return false
    }
}
