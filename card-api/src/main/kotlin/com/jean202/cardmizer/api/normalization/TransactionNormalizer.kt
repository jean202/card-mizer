package com.jean202.cardmizer.api.normalization

import java.util.Locale

class TransactionNormalizer(rules: MerchantNormalizationRules) {
    private val simplePayTags = normalizeValues(rules.simplePayTags)
    private val onlineCategories = normalizeValues(rules.onlineCategories)
    private val offlineCategories = normalizeValues(rules.offlineCategories)
    private val subscriptionCategories = normalizeValues(rules.subscriptionCategories)
    private val digitalContentCategories = normalizeValues(rules.digitalContentCategories)
    private val categoryAliases = normalizeAliases(rules.categoryAliases)
    private val tagAliases = normalizeAliases(rules.tagAliases)
    private val merchantRules = rules.merchantRules.map { MerchantRule.from(it) }

    fun normalize(merchantName: String?, merchantCategory: String?, paymentTags: Set<String>?): NormalizedTransaction {
        val normalizedMerchantName = merchantName?.trim() ?: ""
        val inferredRule = inferMerchantRule(normalizedMerchantName)
        val resolvedCategory = normalizeCategory(merchantCategory)
            ?: inferredRule?.category
            ?: UNCATEGORIZED

        val resolvedTags = LinkedHashSet(normalizeTags(paymentTags))
        inferredRule?.let { mergeInferredTags(it.inferredTags, resolvedTags) }
        addDerivedTags(resolvedCategory, resolvedTags)

        return NormalizedTransaction(resolvedCategory, resolvedTags.toSet())
    }

    private fun normalizeCategory(merchantCategory: String?): String? {
        if (merchantCategory.isNullOrBlank()) return null
        val canonical = canonicalize(merchantCategory)
        return categoryAliases.getOrDefault(canonical, canonical)
    }

    private fun normalizeTags(paymentTags: Set<String>?): Set<String> {
        if (paymentTags.isNullOrEmpty()) return emptySet()
        return paymentTags
            .filter { it.isNotBlank() }
            .map { tagAliases.getOrDefault(canonicalize(it), canonicalize(it)) }
            .toSet()
    }

    private fun inferMerchantRule(merchantName: String): MerchantRule? {
        if (merchantName.isBlank()) return null
        val normalized = merchantName.trim().uppercase(Locale.ROOT)
        return merchantRules.firstOrNull { it.matches(normalized) }
    }

    private fun addDerivedTags(resolvedCategory: String, resolvedTags: LinkedHashSet<String>) {
        if ("ONLINE" !in resolvedTags && "OFFLINE" !in resolvedTags) {
            if (resolvedCategory in onlineCategories) resolvedTags.add("ONLINE")
            if (resolvedCategory in offlineCategories) resolvedTags.add("OFFLINE")
        }

        if (resolvedCategory in subscriptionCategories) resolvedTags.add("SUBSCRIPTION")
        if (resolvedCategory in digitalContentCategories) resolvedTags.add("DIGITAL_CONTENT")

        if (resolvedTags.any { it in simplePayTags }) resolvedTags.add("SIMPLE_PAY")
        if ("SIMPLE_PAY" in resolvedTags && "ONLINE" in resolvedTags) resolvedTags.add("SIMPLE_PAY_ONLINE")
    }

    private fun mergeInferredTags(inferredTags: Set<String>, resolvedTags: LinkedHashSet<String>) {
        val hasExplicitChannel = "ONLINE" in resolvedTags || "OFFLINE" in resolvedTags
        for (tag in inferredTags) {
            if (hasExplicitChannel && (tag == "ONLINE" || tag == "OFFLINE")) continue
            resolvedTags.add(tag)
        }
    }

    private class MerchantRule(val category: String, val keywords: Set<String>, val inferredTags: Set<String>) {
        fun matches(normalizedMerchantName: String) = keywords.any { normalizedMerchantName.contains(it) }

        companion object {
            fun from(rule: MerchantNormalizationRule) = MerchantRule(
                category = canonicalize(rule.category),
                keywords = rule.keywords.map { it.uppercase(Locale.ROOT) }.toSet(),
                inferredTags = normalizeValues(rule.inferredTags),
            )
        }
    }

    companion object {
        private const val UNCATEGORIZED = "UNCATEGORIZED"

        private fun canonicalize(rawValue: String): String =
            rawValue.trim()
                .uppercase(Locale.ROOT)
                .replace("(", " ")
                .replace(")", " ")
                .replace("-", "_")
                .replace("/", "_")
                .replace("+", "_PLUS")
                .replace(Regex("[^A-Z0-9가-힣_ ]"), "")
                .replace(Regex("\\s+"), "_")
                .replace(Regex("_+"), "_")

        private fun normalizeValues(values: Set<String>): Set<String> =
            values.map { canonicalize(it) }.toSet()

        private fun normalizeAliases(aliases: Map<String, String>): Map<String, String> =
            aliases.entries.associate { (k, v) -> canonicalize(k) to canonicalize(v) }
    }
}
