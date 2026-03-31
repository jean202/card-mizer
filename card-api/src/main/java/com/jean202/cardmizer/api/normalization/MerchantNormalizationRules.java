package com.jean202.cardmizer.api.normalization;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record MerchantNormalizationRules(
        Map<String, String> categoryAliases,
        Map<String, String> tagAliases,
        Set<String> simplePayTags,
        Set<String> onlineCategories,
        Set<String> offlineCategories,
        Set<String> subscriptionCategories,
        Set<String> digitalContentCategories,
        List<MerchantNormalizationRule> merchantRules
) {
    public MerchantNormalizationRules {
        categoryAliases = Map.copyOf(Objects.requireNonNull(categoryAliases, "categoryAliases must not be null"));
        tagAliases = Map.copyOf(Objects.requireNonNull(tagAliases, "tagAliases must not be null"));
        simplePayTags = Set.copyOf(Objects.requireNonNull(simplePayTags, "simplePayTags must not be null"));
        onlineCategories = Set.copyOf(Objects.requireNonNull(onlineCategories, "onlineCategories must not be null"));
        offlineCategories = Set.copyOf(Objects.requireNonNull(offlineCategories, "offlineCategories must not be null"));
        subscriptionCategories = Set.copyOf(
                Objects.requireNonNull(subscriptionCategories, "subscriptionCategories must not be null")
        );
        digitalContentCategories = Set.copyOf(
                Objects.requireNonNull(digitalContentCategories, "digitalContentCategories must not be null")
        );
        merchantRules = List.copyOf(Objects.requireNonNull(merchantRules, "merchantRules must not be null"));
    }
}
