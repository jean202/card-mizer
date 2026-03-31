package com.jean202.cardmizer.api.normalization;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TransactionNormalizer {
    private static final String UNCATEGORIZED = "UNCATEGORIZED";
    private final Set<String> simplePayTags;
    private final Set<String> onlineCategories;
    private final Set<String> offlineCategories;
    private final Set<String> subscriptionCategories;
    private final Set<String> digitalContentCategories;
    private final Map<String, String> categoryAliases;
    private final Map<String, String> tagAliases;
    private final List<MerchantRule> merchantRules;

    public TransactionNormalizer() {
        this(new MerchantNormalizationRulesLoader().loadDefault());
    }

    public TransactionNormalizer(MerchantNormalizationRules rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.simplePayTags = normalizeValues(rules.simplePayTags());
        this.onlineCategories = normalizeValues(rules.onlineCategories());
        this.offlineCategories = normalizeValues(rules.offlineCategories());
        this.subscriptionCategories = normalizeValues(rules.subscriptionCategories());
        this.digitalContentCategories = normalizeValues(rules.digitalContentCategories());
        this.categoryAliases = normalizeAliases(rules.categoryAliases());
        this.tagAliases = normalizeAliases(rules.tagAliases());
        this.merchantRules = rules.merchantRules().stream()
                .map(MerchantRule::from)
                .toList();
    }

    public NormalizedTransaction normalize(String merchantName, String merchantCategory, Set<String> paymentTags) {
        String normalizedMerchantName = merchantName == null ? "" : merchantName.trim();
        Optional<MerchantRule> inferredRule = inferMerchantRule(normalizedMerchantName);
        String resolvedCategory = normalizeCategory(merchantCategory)
                .or(() -> inferredRule.map(MerchantRule::category))
                .orElse(UNCATEGORIZED);

        LinkedHashSet<String> resolvedTags = new LinkedHashSet<>(normalizeTags(paymentTags));
        inferredRule.ifPresent(rule -> mergeInferredTags(rule.inferredTags(), resolvedTags));
        addDerivedTags(resolvedCategory, resolvedTags);

        return new NormalizedTransaction(resolvedCategory, Set.copyOf(resolvedTags));
    }

    private Optional<String> normalizeCategory(String merchantCategory) {
        if (merchantCategory == null || merchantCategory.isBlank()) {
            return Optional.empty();
        }
        String canonical = canonicalize(merchantCategory);
        return Optional.of(categoryAliases.getOrDefault(canonical, canonical));
    }

    private Set<String> normalizeTags(Set<String> paymentTags) {
        if (paymentTags == null || paymentTags.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String paymentTag : paymentTags) {
            if (paymentTag == null || paymentTag.isBlank()) {
                continue;
            }
            String canonical = canonicalize(paymentTag);
            normalizedTags.add(tagAliases.getOrDefault(canonical, canonical));
        }
        return Set.copyOf(normalizedTags);
    }

    private Optional<MerchantRule> inferMerchantRule(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return Optional.empty();
        }

        String normalizedMerchantName = merchantName.trim().toUpperCase(Locale.ROOT);
        return merchantRules.stream()
                .filter(rule -> rule.matches(normalizedMerchantName))
                .findFirst();
    }

    private void addDerivedTags(String resolvedCategory, LinkedHashSet<String> resolvedTags) {
        if (!resolvedTags.contains("ONLINE") && !resolvedTags.contains("OFFLINE")) {
            if (onlineCategories.contains(resolvedCategory)) {
                resolvedTags.add("ONLINE");
            }
            if (offlineCategories.contains(resolvedCategory)) {
                resolvedTags.add("OFFLINE");
            }
        }

        if (subscriptionCategories.contains(resolvedCategory)) {
            resolvedTags.add("SUBSCRIPTION");
        }

        if (digitalContentCategories.contains(resolvedCategory)) {
            resolvedTags.add("DIGITAL_CONTENT");
        }

        boolean hasSimplePayTag = resolvedTags.stream().anyMatch(simplePayTags::contains);
        if (hasSimplePayTag) {
            resolvedTags.add("SIMPLE_PAY");
        }

        if (resolvedTags.contains("SIMPLE_PAY") && resolvedTags.contains("ONLINE")) {
            resolvedTags.add("SIMPLE_PAY_ONLINE");
        }
    }

    private void mergeInferredTags(Set<String> inferredTags, LinkedHashSet<String> resolvedTags) {
        boolean hasExplicitChannelTag = resolvedTags.contains("ONLINE") || resolvedTags.contains("OFFLINE");
        for (String inferredTag : inferredTags) {
            if (hasExplicitChannelTag && (inferredTag.equals("ONLINE") || inferredTag.equals("OFFLINE"))) {
                continue;
            }
            resolvedTags.add(inferredTag);
        }
    }

    private static String canonicalize(String rawValue) {
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        return rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace("(", " ")
                .replace(")", " ")
                .replace("-", "_")
                .replace("/", "_")
                .replace("+", "_PLUS")
                .replaceAll("[^A-Z0-9가-힣_ ]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
    }

    private static Set<String> normalizeValues(Set<String> values) {
        return values.stream()
                .map(TransactionNormalizer::canonicalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<String, String> normalizeAliases(Map<String, String> aliases) {
        Map<String, String> normalizedAliases = new LinkedHashMap<>();
        aliases.forEach((alias, canonicalValue) -> normalizedAliases.put(canonicalize(alias), canonicalize(canonicalValue)));
        return Map.copyOf(normalizedAliases);
    }

    private record MerchantRule(String category, Set<String> keywords, Set<String> inferredTags) {
        private static MerchantRule from(MerchantNormalizationRule rule) {
            return new MerchantRule(
                    canonicalize(rule.category()),
                    normalizeKeywordSet(rule.keywords()),
                    normalizeValues(rule.inferredTags())
            );
        }

        private boolean matches(String normalizedMerchantName) {
            return keywords.stream().anyMatch(normalizedMerchantName::contains);
        }

        private static Set<String> normalizeKeywordSet(Set<String> keywords) {
            return keywords.stream()
                    .map(keyword -> keyword.toUpperCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
    }
}
