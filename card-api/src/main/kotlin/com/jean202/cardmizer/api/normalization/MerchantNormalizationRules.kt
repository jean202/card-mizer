package com.jean202.cardmizer.api.normalization

class MerchantNormalizationRules(
    categoryAliases: Map<String, String>,
    tagAliases: Map<String, String>,
    simplePayTags: Set<String>,
    onlineCategories: Set<String>,
    offlineCategories: Set<String>,
    subscriptionCategories: Set<String>,
    digitalContentCategories: Set<String>,
    merchantRules: List<MerchantNormalizationRule>,
) {
    val categoryAliases: Map<String, String> = categoryAliases.toMap()
    val tagAliases: Map<String, String> = tagAliases.toMap()
    val simplePayTags: Set<String> = simplePayTags.toSet()
    val onlineCategories: Set<String> = onlineCategories.toSet()
    val offlineCategories: Set<String> = offlineCategories.toSet()
    val subscriptionCategories: Set<String> = subscriptionCategories.toSet()
    val digitalContentCategories: Set<String> = digitalContentCategories.toSet()
    val merchantRules: List<MerchantNormalizationRule> = merchantRules.toList()
}
